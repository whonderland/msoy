//
// $Id$

package com.threerings.msoy.game.server;

import static com.threerings.msoy.Log.log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.xml.sax.SAXException;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.jdbc.WriteOnlyUnit;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.Invoker;

import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsClient;
import com.threerings.presents.server.ShutdownManager;
import com.threerings.presents.util.PersistingUnit;
import com.threerings.presents.util.ResultListenerList;

import com.threerings.crowd.server.LocationManager;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.server.PlaceManagerDelegate;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.parlor.data.Table;
import com.threerings.parlor.game.data.GameConfig;
import com.threerings.parlor.rating.server.persist.RatingRepository;
import com.threerings.parlor.rating.util.Percentiler;

import com.threerings.bureau.server.BureauRegistry;

import com.whirled.bureau.data.BureauTypes;
import com.whirled.game.data.GameData;
import com.whirled.game.server.PropertySpaceDelegate;
import com.whirled.game.server.PropertySpaceHelper;

import com.threerings.msoy.avrg.client.AVRService;
import com.threerings.msoy.avrg.data.AVRGameConfig;
import com.threerings.msoy.avrg.server.AVRDispatcher;
import com.threerings.msoy.avrg.server.AVRGameManager;
import com.threerings.msoy.avrg.server.AVRProvider;
import com.threerings.msoy.avrg.server.QuestDelegate;
import com.threerings.msoy.avrg.server.persist.AVRGameRepository;
import com.threerings.msoy.avrg.server.persist.GameStateRecord;
import com.threerings.msoy.avrg.server.persist.PlayerGameStateRecord;
import com.threerings.msoy.avrg.server.persist.QuestStateRecord;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.data.GameContentOwnership;
import com.threerings.msoy.game.data.LobbyCodes;
import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.MsoyGameDefinition;
import com.threerings.msoy.game.data.MsoyMatchConfig;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.game.server.persist.TrophyRecord;
import com.threerings.msoy.game.server.persist.TrophyRepository;
import com.threerings.msoy.game.xml.MsoyGameParser;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.ItemPack;
import com.threerings.msoy.item.data.all.LevelPack;
import com.threerings.msoy.item.data.all.Prize;
import com.threerings.msoy.item.data.all.TrophySource;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.GameRepository;
import com.threerings.msoy.item.server.persist.ItemPackRecord;
import com.threerings.msoy.item.server.persist.ItemPackRepository;
import com.threerings.msoy.item.server.persist.LevelPackRecord;
import com.threerings.msoy.item.server.persist.LevelPackRepository;
import com.threerings.msoy.item.server.persist.PrizeRecord;
import com.threerings.msoy.item.server.persist.PrizeRepository;
import com.threerings.msoy.item.server.persist.TrophySourceRecord;
import com.threerings.msoy.item.server.persist.TrophySourceRepository;
import com.threerings.msoy.person.server.persist.FeedRepository;
import com.threerings.msoy.person.util.FeedMessageType;
import com.threerings.msoy.server.MsoyEventLogger;

/**
 * Manages the lobbies active on this server.
 */
@Singleton
public class GameGameRegistry
    implements LobbyProvider, AVRProvider, ShutdownManager.Shutdowner,
    LobbyManager.ShutdownObserver, AVRGameManager.LifecycleObserver
{
    @Inject public GameGameRegistry (ShutdownManager shutmgr, InvocationManager invmgr)
    {
        // register to hear when the server is shutdown
        shutmgr.registerShutdowner(this);

        // register game-related bootstrap services
        invmgr.registerDispatcher(new LobbyDispatcher(this), MsoyCodes.GAME_GROUP);
        invmgr.registerDispatcher(new AVRDispatcher(this), MsoyCodes.WORLD_GROUP);
    }

    /**
     * Provides this registry with an injector it can use to create manager instances.
     */
    public void init (Injector injector)
    {
        _injector = injector;
    }

    /**
     * Returns the game repository used to maintain our persistent data.
     */
    public GameRepository getGameRepository ()
    {
        return _gameRepo;
    }

    /**
     * Returns an enumeration of all of the registered lobby managers.  This should only be
     * accessed on the dobjmgr thread and shouldn't be kept around across event dispatches.
     */
    public Iterator<LobbyManager> enumerateLobbyManagers ()
    {
        return _lobbies.values().iterator();
    }

    /**
     * Returns the percentiler for the specified game and score distribution. The percentiler may
     * be modified and when the lobby for the game in question is finally unloaded, the percentiler
     * will be written back out to the database.
     */
    public Percentiler getScoreDistribution (int gameId, boolean multiplayer)
    {
        return _distribs.get(multiplayer ? Math.abs(gameId) : -Math.abs(gameId));
    }

    /**
     * Resolves the item and level packs owned by the player in question for the specified game, as
     * well as trophies they have earned. This information will show up asynchronously, once the
     */
    public void resolveOwnedContent (final int gameId, final PlayerObject plobj)
    {
        // if we've already resolved content for this player, we are done
        if (plobj.isContentResolved(gameId)) {
            return;
        }

        // add our "already resolved" marker and then start resolving
        plobj.addToGameContent(new GameContentOwnership(gameId, GameData.RESOLVED_MARKER, ""));
        _invoker.postUnit(new RepositoryUnit("resolveOwnedContent") {
            @Override
            public void invokePersist () throws Exception {
                // TODO: load level and item pack ownership
                _trophies = _trophyRepo.loadTrophyOwnership(gameId, plobj.getMemberId());
            }
            @Override
            public void handleSuccess () {
                plobj.startTransaction();
                try {
                    addContent(GameData.LEVEL_DATA, _lpacks);
                    addContent(GameData.ITEM_DATA, _ipacks);
                    addContent(GameData.TROPHY_DATA, _trophies);
                } finally {
                    plobj.commitTransaction();
                }
            }
            protected void addContent (byte type, List<String> idents) {
                for (String ident : idents) {
                    plobj.addToGameContent(new GameContentOwnership(gameId, type, ident));
                }
            }
            @Override
            protected String getFailureMessage () {
                return "Failed to resolve content [game=" + gameId + ", who=" + plobj.who() + "].";
            }

            protected List<String> _lpacks = new ArrayList<String>();
            protected List<String> _ipacks = new ArrayList<String>();
            protected List<String> _trophies;
        });
    }

    /**
     * Called to inform us that our game's content (the GameRecord or any of our sub-items) has
     * been updated. We reload the game's content and inform the lobby so that newly created games
     * will use the new content.
     */
    public void gameContentUpdated (final int gameId)
    {
        // is this a lobbied game?
        final LobbyManager lmgr = _lobbies.get(gameId);
        if (lmgr != null) {
            _invoker.postUnit(new RepositoryUnit("reloadLobby") {
                @Override
                public void invokePersist () throws Exception {
                    // if so, recompile the game content from all its various sources
                    _content = assembleGameContent(gameId);
                }

                @Override
                public void handleSuccess () {
                    // and then update the lobby with the content
                    lmgr.setGameContent(_content);
                    log.info("Reloaded lobbied game configuration [id=" + gameId + "]");
                }

                @Override
                public void handleFailure (Exception e) {
                    // if anything goes wrong, we can just fall back on what was already there
                    log.warning("Failed to resolve game [id=" + gameId + "].", e);
                }

                protected GameContent _content;
            });

            return;
        }

        // see if it's a lobby game we're currently loading...
        ResultListenerList list = _loadingLobbies.get(gameId);
        if (list != null) {
            list.add(new InvocationService.ResultListener() {
                public void requestProcessed (Object result) {
                    gameContentUpdated(gameId);
                }
                public void requestFailed (String cause) {/*ignore*/}
            });
            return;
        }

        // else is it an AVRG?
        AVRGameManager amgr = _avrgManagers.get(gameId);
        if (amgr != null) {
            _invoker.postUnit(new RepositoryUnit("reloadAVRGame") {
                @Override
                public void invokePersist () throws Exception {
                    GameRecord gRec = _gameRepo.loadGameRecord(gameId);
                    if (gRec == null) {
                        throw new Exception("Failed to find GameRecord [gameId=" + gameId + "]");
                    }
//                    _game = (Game) gRec.toItem();
                }

                @Override
                public void handleSuccess () {
// TODO: so what do we do?
//                    amgr.updateGame(_game);
                }

                @Override
                public void handleFailure (Exception pe) {
                    log.warning("Failed to resolve AVRGame [id=" + gameId + "].", pe);
                }

//                protected Game _game;
            });
            return;
        }

        log.warning("Updated game record not, in the end, hosted by us [gameId=" + gameId + "]");
    }

    /**
     * Resets our in-memory percentiler for the specified game. This is triggered by a request from
     * our world server.
     */
    public void resetScorePercentiler (int gameId, boolean single)
    {
        log.info("Resetting in-memory percentiler [gameId=" + gameId + ", single=" + single + "].");
        _distribs.put(single ? -Math.abs(gameId) : Math.abs(gameId), new Percentiler());
    }

    /**
     * Awards the supplied trophy and provides a {@link Trophy} instance on success to the supplied
     * listener or failure.
     */
    public void awardTrophy (final String gameName, final TrophyRecord trophy, String description,
                             InvocationService.ResultListener listener)
    {
        // create the trophy record we'll use to notify them of their award
        final Trophy trec = trophy.toTrophy();
        // fill in the description so that we can report that in the award email
        trec.description = description;

        _invoker.postUnit(new PersistingUnit("awardTrophy", listener) {
            @Override
            public void invokePersistent () throws Exception {
                // store the trophy in the database
                _trophyRepo.storeTrophy(trophy);
                // publish the trophy earning event to the member's feed
                _feedRepo.publishMemberMessage(
                    trophy.memberId, FeedMessageType.FRIEND_WON_TROPHY,
                    trophy.name + "\t" + trophy.gameId +
                    "\t" + MediaDesc.mdToString(trec.trophyMedia));
            }
            @Override
            public void handleSuccess () {
                _worldClient.reportTrophyAward(trophy.memberId, gameName, trec);
                _eventLog.trophyEarned(trophy.memberId, trophy.gameId, trophy.ident);
                _worldClient.incrementStat(trophy.memberId, StatType.TROPHIES_EARNED, 1);
                ((InvocationService.ResultListener)_listener).requestProcessed(trec);
            }
            @Override
            protected String getFailureMessage () {
                return "Failed to store trophy " + trophy + ".";
            }
        });
    }

    /**
     * Called when a game was successfully finished with a payout.
     * Right now just logs the results for posterity.
     */
    public void gamePayout (int memberId, Game game, int payout, int secondsPlayed)
    {
        _eventLog.gamePlayed(
            game.genre, game.gameId, game.itemId, payout, secondsPlayed, memberId);
    }

    // from AVRProvider
    public void activateGame (ClientObject caller, final int gameId,
                              AVRService.AVRGameJoinListener listener)
        throws InvocationException
    {
        // TODO: transition
        // int instanceId = 1;

        PlayerObject player = (PlayerObject) caller;

        InvocationService.ResultListener joinListener =
            new AVRGameJoinListener(player.getMemberId(), listener);

        ResultListenerList list = _loadingAVRGames.get(gameId);
        if (list != null) {
            list.add(joinListener);
            return;
        }

        AVRGameManager mgr = _avrgManagers.get(gameId);
        if (mgr != null) {
            joinListener.requestProcessed(mgr);
            return;
        }

        _loadingAVRGames.put(gameId, list = new ResultListenerList());
        list.add(joinListener);

        _invoker.postUnit(new RepositoryUnit("activateAVRGame") {
            @Override
            public void invokePersist () throws Exception {
                _content = assembleGameContent(gameId);
                if (_content.game != null) {
                    _recs = _avrgRepo.getGameState(gameId);
                }
            }

            @Override
            public void handleSuccess () {
                if (_content.game == null) {
                    reportFailure("m.no_such_game");
                    return;
                }

                MsoyGameDefinition def;
                try {
                    def = (MsoyGameDefinition)new MsoyGameParser().parseGame(_content.game);

                } catch (IOException ioe) {
                    log.warning("Error parsing game config", "game", _content.game, ioe);
                    return;

                } catch (SAXException saxe) {
                    log.warning("Error parsing game config", "game", _content.game, saxe);
                    return;

                }

                List<PlaceManagerDelegate> delegates = Lists.newArrayList();
                // TODO: Move AVRG event logging out of QuestDelegate and maybe into this one?
//                delegates.add(new EventLoggingDelegate(_content));
                // TODO: Refactor the bits of AwardDelegate that we want
//                delegates.add(new AwardDelegate(_content));
                delegates.add(new TrophyDelegate(_content));
                delegates.add(new QuestDelegate(_content));
                delegates.add(new AgentTraceDelegate(gameId));

                final Map<String, byte[]> initialState = new HashMap<String, byte[]>();
                for (GameStateRecord record : _recs) {
                    initialState.put(record.datumKey, record.datumValue);
                }
                delegates.add(new PropertySpaceDelegate() {
                    @Override
                    protected Map<String, byte[]> initialStateFromStore () {
                        return initialState;
                    }
                    @Override
                    protected void writeDirtyStateToStore (final Map<String, byte[]> state) {
                        // the map should be quite safe to pass to another thread
                        _invoker.postUnit(new WriteOnlyUnit("shutdown") {
                            @Override
                            public void invokePersist () throws Exception {
                                for (Map.Entry<String, byte[]> entry : state.entrySet()) {
                                    _avrgRepo.storeState(new GameStateRecord(
                                        gameId, entry.getKey(), entry.getValue()));
                                }
                            }
                        });
                    }
                });

                AVRGameConfig config = new AVRGameConfig();
                config.init(_content.game, def);

                AVRGameManager mgr;
                try {
                    mgr = (AVRGameManager)_plreg.createPlace(config, delegates);

                } catch (Exception e) {
                    log.warning("Failed to create AVRGameObject", "gameId", gameId, e);
                    return;
                }

// TODO: now handled in MsoyGameDefinition
//                mgr.getGameObject().setGameMedia(_content.game.gameMedia);
                mgr.setLifecycleObserver(GameGameRegistry.this);

                // now start up the agent, then wait for the avrGameReady callback
                mgr.startAgent();
                // TODO: add a timeout?
            }

            @Override
            public void handleFailure (Exception pe) {
                log.warning("Failed to resolve AVRGame [id=" + gameId + "].", pe);
                reportFailure(pe.getMessage());
            }

            protected void reportFailure (String reason) {
                ResultListenerList list = _loadingAVRGames.remove(gameId);
                if (list != null) {
                    list.requestFailed(reason);
                } else {
                    log.warning(
                        "No listeners when failing AVRGame [gameId=" + gameId + "]");
                }
            }

            protected GameContent _content;
            protected List<GameStateRecord> _recs;
        });
    }

    // from AVRProvider
    public void deactivateGame (ClientObject caller, int gameId,
                                InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        PlayerObject player = (PlayerObject) caller;
        int playerId = player.getMemberId();

        // see if we are still just resolving the game
        ResultListenerList list = _loadingAVRGames.get(gameId);
        if (list != null) {
            // yep, so just remove our associated listener, and we're done
            for (InvocationService.ResultListener gameListener : list) {
                if (((AVRGameJoinListener) gameListener).getPlayerId() == playerId) {
                    list.remove(gameListener);
                    listener.requestProcessed();
                    return;
                }
            }
        }

        // get the corresponding manager
        AVRGameManager mgr = _avrgManagers.get(gameId);
        if (mgr != null) {
            _locmgr.leavePlace(player);
            // TODO: no longer needed?
            // _worldClient.leaveAVRGame(playerId);

        } else {
            log.warning("Tried to deactivate AVRG without manager [gameId=" + gameId + "]");
        }

        listener.requestProcessed();
    }

    // from LobbyProvider
    public void identifyLobby (ClientObject caller, final int gameId,
                               InvocationService.ResultListener listener)
        throws InvocationException
    {
        // if we're already resolving this lobby, add this listener to the list of those interested
        // in the outcome
        ResultListenerList list = _loadingLobbies.get(gameId);
        if (list != null) {
            list.add(listener);
            return;
        }

        // if the lobby is already resolved, we're good
        LobbyManager mgr = _lobbies.get(gameId);
        if (mgr != null) {
            listener.requestProcessed(mgr.getLobbyObject().getOid());
            return;
        }

        // otherwise we need to do the resolving
        _loadingLobbies.put(gameId, list = new ResultListenerList());
        list.add(listener);

        _invoker.postUnit(new RepositoryUnit("loadLobby") {
            @Override
            public void invokePersist () throws Exception {
                _content = assembleGameContent(gameId);
                // load up the score distribution information for this game as well
                _single = _ratingRepo.loadPercentile(-Math.abs(gameId));
                _multi = _ratingRepo.loadPercentile(Math.abs(gameId));
            }

            @Override
            public void handleSuccess () {
                if (_content.game == null) {
                    reportFailure("m.no_such_game");
                    return;
                }

                LobbyManager lmgr = _injector.getInstance(LobbyManager.class);
                lmgr.init(_invmgr, GameGameRegistry.this);
                lmgr.setGameContent(_content);
                _lobbies.put(gameId, lmgr);

                ResultListenerList list = _loadingLobbies.remove(gameId);
                if (list != null) {
                    list.requestProcessed(lmgr.getLobbyObject().getOid());
                }

                // map this game's score distributions
                _distribs.put(-Math.abs(gameId), _single == null ? new Percentiler() : _single);
                _distribs.put(Math.abs(gameId), _multi == null ? new Percentiler() : _multi);
            }

            @Override
            public void handleFailure (Exception pe) {
                log.warning("Failed to resolve game [id=" + gameId + "].", pe);
                reportFailure(InvocationCodes.E_INTERNAL_ERROR);
            }

            protected void reportFailure (String reason) {
                ResultListenerList list = _loadingLobbies.remove(gameId);
                if (list != null) {
                    list.requestFailed(reason);
                }

                // clear out the hosting record that our world server assigned to us when it sent
                // this client our way to resolve this game
                _worldClient.stoppedHostingGame(gameId);
            }

            protected GameContent _content;
            protected Percentiler _single, _multi;
        });
    }

    // from LobbyProvider
    @SuppressWarnings("fallthrough")
    public void playNow (ClientObject caller, final int gameId, final int mode,
                         final InvocationService.ResultListener listener)
        throws InvocationException
    {
        final PlayerObject plobj = (PlayerObject)caller;
        // we need to make sure the lobby is resolved, so route through identifyLobby
        identifyLobby(caller, gameId, new InvocationService.ResultListener() {
            public void requestProcessed (Object result) {
                // now the lobby should be resolved
                LobbyManager mgr = _lobbies.get(gameId);
                if (mgr == null) {
                    log.warning("identifyLobby() returned non-failure but lobby manager " +
                                "disappeared [gameId=" + gameId + "].");
                    requestFailed(InvocationCodes.E_INTERNAL_ERROR);
                    return;
                }

                // if we're able to get them right into a game, return zero otherwise return the
                // lobby manager oid which will allow the client to display the lobby
                boolean gameCreated;
                switch (mode) {
                case LobbyCodes.PLAY_NOW_IF_SINGLE:
                    MsoyMatchConfig match = (MsoyMatchConfig) mgr.getLobbyObject().gameDef.match;
                    if (match.getMatchType() == GameConfig.PARTY || match.getMaximumPlayers() > 1) {
                        gameCreated = false;
                        break;
                    }
                    // else, fall through to PLAY_NOW_SINGLE

                default:
                case LobbyCodes.PLAY_NOW_SINGLE:
                    gameCreated = mgr.playNowSingle(plobj);
                    break;

                case LobbyCodes.PLAY_NOW_FRIENDS:
                    gameCreated = mgr.playNowMulti(plobj, true);
                    break;

                case LobbyCodes.PLAY_NOW_ANYONE:
                    gameCreated = mgr.playNowMulti(plobj, false);
                    break;
                }
                listener.requestProcessed(gameCreated ? 0 : result);
            }
            public void requestFailed (String cause) {
                listener.requestFailed(cause);
            }
        });
    }

    // from LobbyProvider
    public void joinPlayerGame (ClientObject caller, int playerId,
                                InvocationService.ResultListener listener)
        throws InvocationException
    {
        PlayerObject player = _locator.lookupPlayer(playerId);
        if (player == null) {
            listener.requestFailed("e.player_not_found");
            return;
        }

        // If they're not in a location, we can send that on immediately.
        int placeOid = player.getPlaceOid();
        if (placeOid == -1) {
            listener.requestProcessed(placeOid);
            return;
        }

        // Check to make sure the game that they're in is watchable
        PlaceManager plman = _plreg.getPlaceManager(placeOid);
        if (plman == null) {
            log.warning(
                "Fetched null PlaceManager for player's current gameOid [" + placeOid + "]");
            listener.requestFailed("e.player_not_found");
            return;
        }
        MsoyGameConfig gameConfig = (MsoyGameConfig) plman.getConfig();
        MsoyMatchConfig matchConfig = (MsoyMatchConfig) gameConfig.getGameDefinition().match;
        if (matchConfig.unwatchable) {
            listener.requestFailed("e.unwatchable_game");
            return;
        }

        // Check to make sure the game that they're in is not private
        int gameId = gameConfig.getGameId();
        LobbyManager lmgr = _lobbies.get(gameId);
        if (lmgr == null) {
            log.warning("No lobby manager found for existing game! [" + gameId + "]");
            listener.requestFailed("e.player_not_found");
            return;
        }
        LobbyObject lobj = lmgr.getLobbyObject();
        for (Table table : lobj.tables) {
            if (table.gameOid == placeOid) {
                if (table.tconfig.privateTable) {
                    listener.requestFailed("e.private_game");
                    return;
                }
                break;
            }
        }

        // finally, hand off the game oid
        listener.requestProcessed(placeOid);
    }

    // from interface PresentsServer.Shutdowner
    public void shutdown ()
    {
        // shutdown our active lobbies
        for (LobbyManager lmgr : _lobbies.values().toArray(new LobbyManager[_lobbies.size()])) {
            lobbyDidShutdown(lmgr.getGame().gameId);
        }

        for (AVRGameManager amgr : _avrgManagers.values()) {
            amgr.shutdown(); // this will also call avrGameDidShutdown
        }
    }

    // from interface LobbyManager.ShutdownObserver
    public void lobbyDidShutdown (int gameId)
    {
        // destroy our record of that lobby
        _lobbies.remove(gameId);
        _loadingLobbies.remove(gameId); // just in case

        // kill the bureau session, if any
        killBureauSession(gameId);

        // let our world server know we're audi
        _worldClient.stoppedHostingGame(gameId);

        // flush any modified percentile distributions
        flushPercentiler(-Math.abs(gameId)); // single-player
        flushPercentiler(Math.abs(gameId)); // multiplayer
    }

    // from AVRGameManager.LifecycleObserver
    public void avrGameDidShutdown (AVRGameManager mgr)
    {
        int gameId = mgr.getGameId();

        // destroy our record of that avrg
        _avrgManagers.remove(gameId);
        _loadingAVRGames.remove(gameId);

        // kill the bureau session
        killBureauSession(gameId);

        // let our world server know we're audi
        _worldClient.stoppedHostingGame(gameId);
    }

    // from AVRGameManager.LifecycleObserver
    public void avrGameReady (AVRGameManager mgr)
    {
        int gameId = mgr.getGameId();

        _avrgManagers.put(gameId, mgr);

        ResultListenerList list = _loadingAVRGames.remove(gameId);
        if (list != null) {
            list.requestProcessed(mgr);
        } else {
            log.warning("No listeners when done activating AVRGame", "gameId", gameId);
        }
    }

    protected GameContent assembleGameContent (int gameId)
    {
        GameContent content = new GameContent();
        content.detail = _gameRepo.loadGameDetail(gameId);
        GameRecord rec = _gameRepo.loadGameRecord(gameId, content.detail);
        if (rec != null) {
            content.game = (Game)rec.toItem();
            // load up our level and item packs
            for (LevelPackRecord record :
                     _lpackRepo.loadOriginalItemsBySuite(content.game.getSuiteId())) {
                content.lpacks.add((LevelPack)record.toItem());
            }
            for (ItemPackRecord record :
                     _ipackRepo.loadOriginalItemsBySuite(content.game.getSuiteId())) {
                content.ipacks.add((ItemPack)record.toItem());
            }
            // load up our trophy source items
            for (TrophySourceRecord record :
                     _tsourceRepo.loadOriginalItemsBySuite(content.game.getSuiteId())) {
                content.tsources.add((TrophySource)record.toItem());
            }
            // load up our prize items
            for (PrizeRecord record :
                     _prizeRepo.loadOriginalItemsBySuite(content.game.getSuiteId())) {
                content.prizes.add((Prize)record.toItem());
            }
        }
        return content;
    }

    protected void joinAVRGame (final int playerId, final AVRGameManager mgr,
                                final AVRService.AVRGameJoinListener listener)
    {
        _invoker.postUnit(new RepositoryUnit("joinAVRGame") {
            @Override
            public void invokePersist () throws Exception {
                _questRecs = _avrgRepo.getQuests(mgr.getGameId(), playerId);
                _stateRecs = _avrgRepo.getPlayerGameState(mgr.getGameId(), playerId);
            }
            @Override
            public void handleSuccess () {
                PlayerObject player = _locator.lookupPlayer(playerId);
                if (player == null) {
                    // they left while we were resolving the game, oh well
                    return;
                }

                if (!MemberName.isGuest(player.getMemberId())) {
                    Map<String, byte[]> initialState = new HashMap<String, byte[]>();
                    for (PlayerGameStateRecord record : _stateRecs) {
                        initialState.put(record.datumKey, record.datumValue);
                    }
                    PropertySpaceHelper.initWithStateFromStore(player, initialState);

                    player.startTransaction();
                    try {
                        for (QuestStateRecord rec: _questRecs) {
                            player.addToQuestState(rec.toEntry());
                        }
                    } finally {
                        player.commitTransaction();
                    }
                }

                int gameOid = mgr.getGameObject().getOid();
                // when we're ready, move the player into the AVRG 'place'
                try {
                    _locmgr.moveTo(player, gameOid);

                } catch (InvocationException pe) {
                    log.warning("Move to AVRGameObject failed", "gameId", mgr.getGameId(), pe);
                    listener.requestFailed(InvocationCodes.E_INTERNAL_ERROR);
                    return;
                }

                // if all went well, return the AVRGameConfig to the client
                listener.avrgJoined(gameOid, (AVRGameConfig) mgr.getConfig());
            }
            @Override
            public void handleFailure (Exception pe) {
                log.warning("Unable to resolve player state [gameId=" +
                    mgr.getGameId() + ", player=" + playerId + "]", pe);
            }

            protected List<QuestStateRecord> _questRecs;
            protected List<PlayerGameStateRecord> _stateRecs;
        });
    }

    protected void flushPercentiler (final int gameId)
    {
        final Percentiler tiler = _distribs.remove(gameId);
        if (tiler == null || !tiler.isModified()) {
            return;
        }

        _invoker.postUnit(new Invoker.Unit("flushPercentiler") {
            @Override
            public boolean invoke () {
                try {
                    _ratingRepo.updatePercentile(gameId, tiler);
                } catch (Exception e) {
                    log.warning("Failed to update score distribution", "game", gameId,
                                "tiler", tiler, e);
                }
                return false;
            }
        });
    }

    protected void killBureauSession (int gameId)
    {
        String bureauId = BureauTypes.GAME_BUREAU_ID_PREFIX + gameId;
        PresentsClient bureau = _bureauReg.lookupClient(bureauId);
        if (bureau != null) {
            bureau.endSession();
        }
    }

    protected class AVRGameJoinListener
        implements InvocationService.ResultListener
    {
        protected AVRGameJoinListener (int player, AVRService.AVRGameJoinListener listener)
        {
            _listener = listener;
            _player = player;
        }

        public int getPlayerId ()
        {
            return _player;
        }

        public void requestProcessed (Object result) {
            joinAVRGame(_player, (AVRGameManager) result, _listener);
        }

        public void requestFailed (String cause) {
            _listener.requestFailed(cause);
        }

        protected int _player;
        protected AVRService.AVRGameJoinListener _listener;
    }

    /** Maps game id -> lobby. */
    protected IntMap<LobbyManager> _lobbies = new HashIntMap<LobbyManager>();

    /** Maps game id -> a mapping of various percentile distributions. */
    protected IntMap<Percentiler> _distribs = new HashIntMap<Percentiler>();

    /** Maps game id -> listeners waiting for a lobby to load. */
    protected IntMap<ResultListenerList> _loadingLobbies = new HashIntMap<ResultListenerList>();

    /** Maps game id -> manager for AVR games. */
    protected IntMap<AVRGameManager> _avrgManagers = new HashIntMap<AVRGameManager>();

    /** Maps game id -> listeners waiting for a lobby to load. */
    protected IntMap<ResultListenerList> _loadingAVRGames = new HashIntMap<ResultListenerList>();

    /** We use this to inject dependencies into managers we create. */
    protected Injector _injector;

    // various and sundry dependent services
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected InvocationManager _invmgr;
    @Inject protected GameWatcherManager _watchmgr;
    @Inject protected LocationManager _locmgr;
    @Inject protected PlaceRegistry _plreg;
    @Inject protected WorldServerClient _worldClient;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected PlayerLocator _locator;
    @Inject protected BureauRegistry _bureauReg;

    // various and sundry repositories for loading persistent data
    @Inject protected GameRepository _gameRepo;
    @Inject protected AVRGameRepository _avrgRepo;
    @Inject protected RatingRepository _ratingRepo;
    @Inject protected FeedRepository _feedRepo;
    @Inject protected TrophyRepository _trophyRepo;
    @Inject protected LevelPackRepository _lpackRepo;
    @Inject protected ItemPackRepository _ipackRepo;
    @Inject protected TrophySourceRepository _tsourceRepo;
    @Inject protected PrizeRepository _prizeRepo;
}
