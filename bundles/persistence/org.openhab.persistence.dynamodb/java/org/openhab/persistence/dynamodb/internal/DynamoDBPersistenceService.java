/**
 * Copyright (c) 2010-2016, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.persistence.dynamodb.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.persistence.FilterCriteria;
import org.openhab.core.persistence.FilterCriteria.Operator;
import org.openhab.core.persistence.FilterCriteria.Ordering;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.QueryablePersistenceService;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.PaginationLoadingStrategy;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TableStatus;
import com.google.common.collect.ImmutableMap;

/**
 * This is the implementation of the DynamoDB {@link PersistenceService}. It persists item values
 * using the <a href="https://aws.amazon.com/dynamodb/">Amazon DynamoDB</a> database. The states (
 * {@link State}) of an {@link Item} are persisted in a time series with names equal to the name of
 * the item. All values are stored using integers or doubles, {@link OnOffType} and
 * {@link OpenClosedType} are stored using 0 or 1.
 *
 * The default database name is "openhab"
 *
 * @author Sami Salonen
 *
 */
public class DynamoDBPersistenceService implements QueryablePersistenceService {

    private ItemRegistry itemRegistry;
    private DynamoDBClient db;
    private static final Logger logger = LoggerFactory.getLogger(DynamoDBPersistenceService.class);
    private boolean isProperlyConfigured;
    private DynamoDBConfig dbConfig;
    private DynamoDBTableNameResolver tableNameResolver;

    /**
     * For testing. Allows access to underlying DynamoDBClient.
     *
     * @return DynamoDBClient connected to AWS Dyanamo DB.
     */
    DynamoDBClient getDb() {
        return db;
    }

    public void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    public void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

    public void activate(final BundleContext bundleContext, final Map<String, Object> config) {
        resetClient();
        dbConfig = DynamoDBConfig.fromConfig(config);
        if (dbConfig == null) {
            // Configuration was invalid. Abort service activation.
            // Error is already logger in fromConfig.
            return;
        }

        tableNameResolver = new DynamoDBTableNameResolver(dbConfig.getTablePrefix());
        try {
            boolean connectionOK = maybeConnectAndCheckConnection();
            if (db == null) {
                logger.error("Error creating dynamodb database client. Aborting service activation.");
                return;
            }
            if (!connectionOK) {
                // client creation succeeded but connection is not OK.
                logger.warn("Failed to establish the dynamodb database connection. "
                        + "Connection will be retried later on persistence service query/store.");
            }
        } catch (Exception e) {
            logger.error("Error constructing dynamodb client", e);
            return;
        }
        isProperlyConfigured = true;
        logger.debug("dynamodb persistence service activated");
    }

    public void deactivate() {
        logger.debug("dynamodb persistence service deactivated");
        resetClient();
    }

    /**
     * Initializes DynamoDBClient (db field), if necessary, and checks the connection.
     *
     * If DynamoDBClient constructor throws an exception, error is logged and false is returned.
     *
     * @return whether connection was successful.
     */
    private boolean maybeConnectAndCheckConnection() {
        if (db == null) {
            try {
                db = new DynamoDBClient(dbConfig);
            } catch (Exception e) {
                logger.error("Error constructing dynamodb client", e);
                return false;
            }
        }
        return db.checkConnection();
    }

    /**
     * Create table (if not present) and wait for table to become active.
     *
     * Synchronized in order to ensure that at most single thread is creating the table at a time
     *
     * @param mapper
     * @param dtoClass
     * @return whether table creation succeeded.
     */
    private synchronized boolean createTable(DynamoDBMapper mapper, Class<?> dtoClass) {
        if (db == null) {
            return false;
        }
        String tableName;
        try {
            ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput(dbConfig.getReadCapacityUnits(),
                    dbConfig.getWriteCapacityUnits());
            CreateTableRequest request = mapper.generateCreateTableRequest(dtoClass);
            request.setProvisionedThroughput(provisionedThroughput);
            if (request.getGlobalSecondaryIndexes() != null) {
                for (GlobalSecondaryIndex index : request.getGlobalSecondaryIndexes()) {
                    index.setProvisionedThroughput(provisionedThroughput);
                }
            }
            tableName = request.getTableName();
            try {
                db.getDynamoClient().describeTable(tableName);
            } catch (ResourceNotFoundException e) {
                // No table present, continue with creation
                db.getDynamoClient().createTable(request);
            } catch (AmazonClientException e) {
                logger.error("Table creation failed due to error in describeTable operation", e);
                return false;
            }

            // table found or just created, wait
            return waitForTableToBecomeActive(tableName);

        } catch (AmazonClientException e) {
            logger.error("Exception when creating table", e);
            return false;
        }

    }

    private boolean waitForTableToBecomeActive(String tableName) {
        try {
            logger.debug("Checking if table '{}' is created...", tableName);
            TableDescription tableDescription = db.getDynamoDB().getTable(tableName).waitForActiveOrDelete();
            if (tableDescription == null) {
                // table has been deleted
                logger.warn("Table '{}' deleted unexpectedly", tableName);
                return false;
            }
            boolean success = TableStatus.ACTIVE.equals(TableStatus.fromValue(tableDescription.getTableStatus()));
            if (success) {
                logger.debug("Creation of table '{}' successful, table status is now {}", tableName,
                        tableDescription.getTableStatus());
            } else {
                logger.warn("Creation of table '{}' unsuccessful, table status is now {}", tableName,
                        tableDescription.getTableStatus());
            }
            return success;
        } catch (AmazonClientException e) {
            logger.error("Exception when checking table status (describe): {}", e.getMessage());
            return false;
        } catch (InterruptedException e) {
            logger.error("Interrupted while trying to check table status: {}", e.getMessage());
            return false;
        }
    }

    private void resetClient() {
        if (db == null) {
            return;
        }
        db.shutdown();
        db = null;
        dbConfig = null;
        tableNameResolver = null;
        isProperlyConfigured = false;
    }

    private DynamoDBMapper getDBMapper(String tableName) {
        try {
            DynamoDBMapperConfig mapperConfig = new DynamoDBMapperConfig.Builder()
                    .withTableNameOverride(new DynamoDBMapperConfig.TableNameOverride(tableName))
                    .withPaginationLoadingStrategy(PaginationLoadingStrategy.LAZY_LOADING).build();
            return new DynamoDBMapper(db.getDynamoClient(), mapperConfig);
        } catch (AmazonClientException e) {
            logger.error("Error getting db mapper: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public String getName() {
        return "dynamodb";
    }

    @Override
    public void store(Item item) {
        store(item, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void store(Item item, String alias) {
        if (item.getState() instanceof UnDefType) {
            logger.debug("Undefined item state received. Not storing item.");
            return;
        }
        if (!isProperlyConfigured) {
            logger.warn("Configuration for dynamodb not yet loaded or broken. Not storing item.");
            return;
        }
        if (!maybeConnectAndCheckConnection()) {
            logger.warn("DynamoDB not connected. Not storing item.");
            return;
        }
        String realName = item.getName();
        String name = (alias != null) ? alias : realName;
        Date time = new Date(System.currentTimeMillis());

        State state = item.getState();
        logger.trace("Tried to get item from item class {}, state is {}", item.getClass(), state.toString());
        DynamoDBItem<?> dynamoItem = AbstractDynamoDBItem.fromState(name, state, time);
        DynamoDBMapper mapper = getDBMapper(tableNameResolver.fromItem(dynamoItem));

        if (!createTable(mapper, dynamoItem.getClass())) {
            logger.warn("Table creation failed. Not storing item");
            return;
        }

        try {
            logger.debug("storing {} in dynamo. Serialized value {}. Original Item: {}", name, state, item);
            mapper.save(dynamoItem);
            logger.debug("Sucessfully stored item {}", item);
        } catch (AmazonClientException e) {
            logger.error("Error storing object to dynamo: {}", e.getMessage());
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<HistoricItem> query(FilterCriteria filter) {
        logger.debug("got a query");
        if (!isProperlyConfigured) {
            logger.warn("Configuration for dynamodb not yet loaded or broken. Not storing item.");
            return Collections.emptyList();
        }
        if (!maybeConnectAndCheckConnection()) {
            logger.warn("DynamoDB not connected. Not storing item.");
            return Collections.emptyList();
        }

        String itemName = filter.getItemName();
        Item item = getItemFromRegistry(itemName);
        if (item == null) {
            logger.warn("Could not get item {} from registry!", itemName);
            return Collections.emptyList();
        }

        Class<DynamoDBItem<?>> dtoClass = AbstractDynamoDBItem.getDynamoItemClass(item.getClass());
        String tableName = tableNameResolver.fromClass(dtoClass);
        DynamoDBMapper mapper = getDBMapper(tableName);
        logger.debug("item {} (class {}) will be tried to query using dto class {} from table {}", itemName,
                item.getClass(), dtoClass, tableName);

        List<HistoricItem> historicItems = new ArrayList<HistoricItem>();

        DynamoDBQueryExpression<DynamoDBItem<?>> queryExpression = createQueryExpression(dtoClass, filter);
        @SuppressWarnings("rawtypes")
        final PaginatedQueryList<? extends DynamoDBItem> paginatedList;
        try {
            paginatedList = mapper.query(dtoClass, queryExpression);
        } catch (AmazonServiceException e) {
            logger.error(
                    "DynamoDB query raised unexpected exception: {}. Returning empty collection. "
                            + "Status code 400 (resource not found) might occur if table was just created.",
                    e.getMessage());
            return Collections.emptyList();
        }
        for (int itemIndexOnPage = 0; itemIndexOnPage < filter.getPageSize(); itemIndexOnPage++) {
            int itemIndex = filter.getPageNumber() * filter.getPageSize() + itemIndexOnPage;
            DynamoDBItem<?> dynamoItem;
            try {
                dynamoItem = paginatedList.get(itemIndex);
            } catch (IndexOutOfBoundsException e) {
                logger.debug("Index {} is out-of-bounds", itemIndex);
                break;
            }
            if (dynamoItem != null) {
                HistoricItem historicItem = dynamoItem.asHistoricItem(item);
                logger.trace("Dynamo item {} converted to historic item: {}", item, historicItem);
                historicItems.add(historicItem);
            }

        }
        return historicItems;
    }

    /**
     * Construct dynamodb query from filter
     *
     * @param filter
     * @return DynamoDBQueryExpression corresponding to the given FilterCriteria
     */
    private DynamoDBQueryExpression<DynamoDBItem<?>> createQueryExpression(Class<? extends DynamoDBItem<?>> dtoClass,
            FilterCriteria filter) {
        DynamoDBItem<?> item = getDynamoDBHashKey(dtoClass, filter.getItemName());
        final DynamoDBQueryExpression<DynamoDBItem<?>> queryExpression = new DynamoDBQueryExpression<DynamoDBItem<?>>()
                .withHashKeyValues(item).withScanIndexForward(filter.getOrdering() == Ordering.ASCENDING)
                .withLimit(filter.getPageSize());
        Condition timeFilter = maybeAddTimeFilter(queryExpression, filter);
        maybeAddStateFilter(filter, queryExpression);
        logger.debug("Querying: {} with {}", filter.getItemName(), timeFilter);
        return queryExpression;
    }

    private DynamoDBItem<?> getDynamoDBHashKey(Class<? extends DynamoDBItem<?>> dtoClass, String itemName) {
        DynamoDBItem<?> item;
        try {
            item = dtoClass.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        item.setName(itemName);
        return item;
    }

    private void maybeAddStateFilter(FilterCriteria filter,
            final DynamoDBQueryExpression<DynamoDBItem<?>> queryExpression) {
        if (filter.getOperator() != null && filter.getState() != null) {
            // Convert filter's state to DynamoDBItem in order get suitable string representation for the state
            final DynamoDBItem<?> filterState = AbstractDynamoDBItem.fromState(filter.getItemName(), filter.getState(),
                    new Date());
            queryExpression.setFilterExpression(String.format("%s %s :opstate", DynamoDBItem.ATTRIBUTE_NAME_ITEMSTATE,
                    operatorAsString(filter.getOperator())));

            filterState.accept(new DynamoDBItemVisitor() {

                @Override
                public void visit(DynamoDBStringItem dynamoStringItem) {
                    queryExpression.setExpressionAttributeValues(
                            ImmutableMap.of(":opstate", new AttributeValue().withS(dynamoStringItem.getState())));
                }

                @Override
                public void visit(DynamoDBBigDecimalItem dynamoBigDecimalItem) {
                    queryExpression.setExpressionAttributeValues(ImmutableMap.of(":opstate",
                            new AttributeValue().withN(dynamoBigDecimalItem.getState().toPlainString())));
                }
            });

        }
    }

    private Condition maybeAddTimeFilter(final DynamoDBQueryExpression<DynamoDBItem<?>> queryExpression,
            final FilterCriteria filter) {
        final Condition timeCondition = constructTimeCondition(filter);
        if (timeCondition != null) {
            queryExpression.setRangeKeyConditions(
                    Collections.singletonMap(DynamoDBItem.ATTRIBUTE_NAME_TIMEUTC, timeCondition));
        }
        return timeCondition;
    }

    private Condition constructTimeCondition(FilterCriteria filter) {
        boolean hasBegin = filter.getBeginDate() != null;
        boolean hasEnd = filter.getEndDate() != null;

        final Condition timeCondition;
        if (!hasBegin && !hasEnd) {
            timeCondition = null;
        } else if (!hasBegin && hasEnd) {
            timeCondition = new Condition().withComparisonOperator(ComparisonOperator.LE).withAttributeValueList(
                    new AttributeValue().withS(AbstractDynamoDBItem.DATEFORMATTER.format(filter.getEndDate())));
        } else if (hasBegin && !hasEnd) {
            timeCondition = new Condition().withComparisonOperator(ComparisonOperator.GE).withAttributeValueList(
                    new AttributeValue().withS(AbstractDynamoDBItem.DATEFORMATTER.format(filter.getBeginDate())));
        } else {
            timeCondition = new Condition().withComparisonOperator(ComparisonOperator.BETWEEN).withAttributeValueList(
                    new AttributeValue().withS(AbstractDynamoDBItem.DATEFORMATTER.format(filter.getBeginDate())),
                    new AttributeValue().withS(AbstractDynamoDBItem.DATEFORMATTER.format(filter.getEndDate())));
        }
        return timeCondition;
    }

    /**
     * Convert op to string suitable for dynamodb filter expression
     *
     * @param op
     * @return string representation corresponding to the given the Operator
     */
    private static String operatorAsString(Operator op) {
        switch (op) {
            case EQ:
                return "=";
            case NEQ:
                return "<>";
            case LT:
                return "<";
            case LTE:
                return "<=";
            case GT:
                return ">";
            case GTE:
                return ">=";

            default:
                throw new IllegalStateException("Unknown operator " + op);
        }
    }

    /**
     * Retrieves the item for the given name from the item registry
     *
     * @param itemName
     * @return item with the given name, or null if no such item exists in item registry.
     */
    private Item getItemFromRegistry(String itemName) {
        Item item = null;
        try {
            if (itemRegistry != null) {
                item = itemRegistry.getItem(itemName);
            }
        } catch (ItemNotFoundException e1) {
            logger.error("Unable to get item {} from registry", itemName);
        }
        return item;
    }

}
