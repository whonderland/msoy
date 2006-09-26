//
// $Id$

package com.threerings.msoy.item.server;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.RepositoryListenerUnit;
import com.samskivert.jdbc.depot.DepotMarshaller;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.util.ResultListener;
import com.samskivert.util.SoftCache;
import com.samskivert.util.Tuple;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService.ConfirmListener;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.web.server.ServletWaiter;

import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.DocumentRepository;
import com.threerings.msoy.item.server.persist.FurnitureRepository;
import com.threerings.msoy.item.server.persist.GameRepository;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.item.server.persist.PhotoRepository;
import com.threerings.msoy.item.server.persist.TagNameRecord;
import com.threerings.msoy.item.server.persist.TagRecord;
import com.threerings.msoy.item.util.ItemEnum;
import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.Item;

import static com.threerings.msoy.Log.log;

/**
 * Manages digital items and their underlying repositories.
 */
public class ItemManager
    implements ItemProvider
{
    /**
     * Initializes the item manager, which will establish database connections
     * for all of its item repositories.
     */
    @SuppressWarnings("unchecked")
    public void init (ConnectionProvider conProv) throws PersistenceException
    {
        _repos.put(ItemEnum.DOCUMENT,
            (ItemRepository) new DocumentRepository(conProv));
        _repos.put(ItemEnum.FURNITURE,
            (ItemRepository) new FurnitureRepository(conProv));
        _repos.put(ItemEnum.GAME,
            (ItemRepository) new GameRepository(conProv));
        _repos.put(ItemEnum.PHOTO,
            (ItemRepository) new PhotoRepository(conProv));

        // register our invocation service
        MsoyServer.invmgr.registerDispatcher(new ItemDispatcher(this), true);
    }

    // from ItemProvider
    public void getInventory (ClientObject caller, String type,
            final InvocationService.ResultListener listener)
        throws InvocationException
    {
        MemberObject memberObj = (MemberObject) caller;
        if (memberObj.isGuest()) {
            throw new InvocationException(InvocationCodes.ACCESS_DENIED);
        }
        // go ahead and throw a RuntimeException if 'type' is bogus
        ItemEnum etype = Enum.valueOf(ItemEnum.class, type);

        // then, load that type
        // TODO: not everything!
        loadInventory(
            memberObj.getMemberId(), etype,
            new ResultListener<ArrayList<Item>>() {
                public void requestCompleted (ArrayList<Item> result)
                {
                    Item[] items = new Item[result.size()];
                    result.toArray(items);
                    listener.requestProcessed(items);
                }

                public void requestFailed (Exception cause)
                {
                    log.warning("Unable to retrieve inventory " + "[cause="
                        + cause + "].");
                    listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
                }
            });
    }

    /**
     * Get the specified item.
     */
    public void getItem (
        ItemEnum type, final int itemId, ResultListener<Item> listener)
    {
        final ItemRepository<ItemRecord> repo = _repos.get(type);
        if (repo == null) {
            listener.requestFailed(new Exception("No repository registered " +
                "for " + type + "."));
            return;
        }

        // TODO: do we have to check cloned items as well?
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Item>(listener) {
            public Item invokePersistResult ()
                throws PersistenceException
            {
                return repo.loadItem(itemId).toItem();
            }
        });
    }

    /**
     * Inserts the supplied item into the system. The item should be fully
     * configured, and an item id will be assigned during the insertion
     * process. Success or failure will be communicated to the supplied result
     * listener.
     */
    public void insertItem (final Item item, ResultListener<Item> waiter)
    {
        final ItemRecord record = ItemRecord.newRecord(item); 
        ItemEnum type = record.getType();

        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = _repos.get(type);
        if (repo == null) {
            waiter.requestFailed(new Exception("No repository registered for "
                + type + "."));
            return;
        }

        // and insert the item; notifying the listener on success or failure
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Item>(
            waiter) {
            public Item invokePersistResult ()
                throws PersistenceException
            {
                repo.insertItem(record);
                item.itemId = record.itemId;
                return item;
            }

            public void handleSuccess ()
            {
                super.handleSuccess();
                // add the item to the user's cached inventory
                updateUserCache(record);
            }
        });
    }

    /**
     * Loads up the inventory of items of the specified type for the specified
     * member. The results may come from the cache and will be cached after
     * being loaded from the database.
     */
    public void loadInventory (final int memberId, ItemEnum type,
            ResultListener<ArrayList<Item>> waiter)
    {
        // first check the cache
        final Tuple<Integer, ItemEnum> key =
            new Tuple<Integer, ItemEnum>(memberId, type);
//      TODO: Disable cache for the moment
        if (false) {
        Collection<ItemRecord> items = _itemCache.get(key);
        if (items != null) {
            ArrayList<Item> list = new ArrayList<Item>();
            for (ItemRecord record : items) {
                list.add(record.toItem());
            }
            waiter.requestCompleted(list);
            return;
        }
        }
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = _repos.get(type);
        if (repo == null) {
            waiter.requestFailed(new Exception("No repository registered for "
                + type + "."));
            return;
        }

        // and load their items; notifying the listener on success or failure
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<ArrayList<Item>>(waiter) {
                    public ArrayList<Item> invokePersistResult ()
                        throws PersistenceException
                    {
                        Collection<ItemRecord> list =
                            repo.loadOriginalItems(memberId);
                        list.addAll(repo.loadClonedItems(memberId));
                        ArrayList<Item> newList = new ArrayList<Item>();
                        for (ItemRecord record : list) {
                            newList.add(record.toItem());
                        }
                        return newList;
                    }

                    public void handleSuccess ()
                    {
// TODO: The cache needs some rethinking, I figure.
//                        _itemCache.put(key, _result);
                        super.handleSuccess();
                    }
                });
    }

    /**
     * Fetches the entire catalog of listed items of the given type.
     */
    public void loadCatalog (int memberId, ItemEnum type,
            ResultListener<ArrayList<CatalogListing>> waiter)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = _repos.get(type);
        if (repo == null) {
            waiter.requestFailed(new Exception("No repository registered for "
                + type + "."));
            return;
        }

        // and load the catalog
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<ArrayList<CatalogListing>>(waiter) {
                public ArrayList<CatalogListing> invokePersistResult ()
                    throws PersistenceException
                {
                    ArrayList<CatalogListing> list =
                        new ArrayList<CatalogListing>();
                    for (CatalogRecord record : repo.loadCatalog()) {
                        list.add(record.toListing());
                    }
                    return list;
                }
            });
    }

    /**
     * Purchases a given item for a given member from the catalog by
     * creating a new clone row in the appropriate database table.
     */
    public void purchaseItem (final int memberId, final int itemId,
            ItemEnum type, ResultListener<Item> waiter)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = _repos.get(type);
        if (repo == null) {
            waiter.requestFailed(new Exception("No repository registered for "
                + type + "."));
            return;
        }

        // and perform the purchase
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Item>(waiter) {
            public Item invokePersistResult () throws PersistenceException
            {
                // load the item being purchased
                ItemRecord item = repo.loadItem(itemId);
                // sanity check it
                if (item.ownerId != -1) {
                    throw new PersistenceException(
                        "Can only clone listed items [itemId=" +
                        item.itemId + "]");
                }
                // create the clone row in the database!
                int cloneId = repo.insertClone(item.itemId, memberId);
                // then dress the loaded item up as a clone
                item.ownerId = memberId;
                item.parentId = item.itemId;
                item.itemId = cloneId;
                return item.toItem();
            }
        });
    }

    /**
     * Lists the given item in the catalog by creating a new item row and
     * a new catalog row and returning the immutable form of the item.
     */

    public void listItem (final int itemId, ItemEnum type,
            ResultListener<CatalogListing> waiter)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = _repos.get(type);
        if (repo == null) {
            waiter.requestFailed(new Exception("No repository registered for "
                + type + "."));
            return;
        }

        // and perform the listing
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<CatalogListing>(waiter) {
            public CatalogListing invokePersistResult ()
                throws PersistenceException
            {
                // load a copy of the original item
                ItemRecord listItem = repo.loadItem(itemId);
                if (listItem == null) {
                    throw new PersistenceException(
                        "Can't find object to list [itemId = " + itemId + "]");
                }
                if (listItem.ownerId == -1) {
                    throw new PersistenceException(
                        "Object is already listed [itemId=" + itemId + "]");
                }
                // reset the owner
                listItem.ownerId = -1;
                // and the iD
                listItem.itemId = 0;
                // then insert it as the immutable copy we list
                repo.insertItem(listItem);
                // and finally create & insert the catalog record
                CatalogRecord record = repo.insertListing(
                    listItem, new Timestamp(System.currentTimeMillis()));
                return record.toListing();
            }
        });
    }

    /**
     * Remix a clone, turning it back into a full-featured original.
     */
    public void remixItem (final int itemId, ItemEnum type,
            ResultListener<Item> waiter)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = _repos.get(type);
        if (repo == null) {
            waiter.requestFailed(new Exception("No repository registered for "
                + type + "."));
            return;
        }
        // and perform the remixing
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Item>(waiter) {
            public Item invokePersistResult () throws PersistenceException
            {
                // load a copy of the clone to modify
                _item = repo.loadClone(itemId);
                // make it ours
                _item.creatorId = _item.ownerId;
                // forget whence it came
                _item.parentId = -1;
                // insert it as a genuinely new item
                _item.itemId = 0;
                repo.insertItem(_item);
                // and finally delete the old clone
                repo.deleteClone(itemId);
                return _item.toItem();
            }

            public void handleSuccess ()
            {
                super.handleSuccess();
                // add the item to the user's cached inventory
                updateUserCache(_item);
            }

            protected ItemRecord _item;
        });

    }

    // TODO: copy on remix
    /** Add the specified tag to the specified item. */
    public void tagItem (
            int itemId, ItemEnum type, int taggerId, String tagName,
            ResultListener<Void> waiter)
    {
        itemTagging(itemId, type, taggerId, tagName, waiter, true);
    }

    /** Remove the specified tag from the specified item. */
    public void untagItem (
            int itemId, ItemEnum type, int taggerId, String tagName,
            ResultListener<Void> waiter)
    {
        itemTagging(itemId, type, taggerId, tagName, waiter, false);
    }

    // do the facade work for tagging
    protected void itemTagging (
            final int itemId, ItemEnum type, final int taggerId,
            final String tagName, ResultListener<Void> waiter,
            final boolean doTag)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = _repos.get(type);
        if (repo == null) {
            waiter.requestFailed(new Exception("No repository registered for "
                + type + "."));
            return;
        }
        // and perform the remixing
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Void>(waiter) {
                public Void invokePersistResult () throws PersistenceException {
                    long now = System.currentTimeMillis();

                    ItemRecord item = repo.loadItem(itemId);
                    int originalId;
                    if (item == null) {
                        item = repo.loadClone(itemId);
                        if (item == null) {
                            throw new PersistenceException(
                                "Can't find item [itemId=" + itemId + "]");
                        }
                        originalId = item.parentId;
                    } else {
                        originalId = itemId;
                    }
                    TagNameRecord tag = repo.getTag(tagName);
                    if (doTag) {
                        repo.tagItem(originalId, tag.tagId, taggerId, now);
                    } else {
                        repo.untagItem(originalId, tag.tagId, taggerId, now);
                    }
                    return null;
            }
        });

    }

    /**
     * Called when an item is newly created and should be inserted into the
     * owning user's inventory cache.
     */
    protected void updateUserCache (ItemRecord item)
    {
        ItemEnum type = item.getType();
        Collection<ItemRecord> items =
            _itemCache.get(new Tuple<Integer, ItemEnum>(item.ownerId, type));
        if (items != null) {
            items.add(item);
        }
    }

    /** Maps string identifier to repository for all digital item types. */
    protected HashMap<ItemEnum, ItemRepository<ItemRecord>> _repos =
        new HashMap<ItemEnum, ItemRepository<ItemRecord>>();

    /** A soft reference cache of item list indexed on (user,type). */
    protected SoftCache<Tuple<Integer, ItemEnum>, Collection<ItemRecord>> _itemCache =
        new SoftCache<Tuple<Integer, ItemEnum>, Collection<ItemRecord>>();
}
