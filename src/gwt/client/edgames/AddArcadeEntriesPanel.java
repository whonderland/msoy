//
// $Id$

package client.edgames;

import java.util.HashMap;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.edgame.gwt.EditGameService;
import com.threerings.msoy.edgame.gwt.EditGameServiceAsync;
import com.threerings.msoy.game.gwt.ArcadeData;
import com.threerings.msoy.game.gwt.GameGenre;
import com.threerings.msoy.game.gwt.GameInfo.Sort;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.web.gwt.Pages;

import client.game.GameGenrePanel;
import client.util.ClickCallback;
import client.util.InfoCallback;
import client.util.Link;

/**
 * User interface for adding games to an arcade. This is mostly the same as a game genre list
 * with genre ALL except the right hand column shows an add or remove button instead of a play
 * button.
 */
public class AddArcadeEntriesPanel extends GameGenrePanel
{
    public AddArcadeEntriesPanel (ArcadeData.Portal page, final Sort sort, String query)
    {
        // we need to use the main, unfiltered portal here so new games can be added
        super(ArcadeData.Portal.MAIN, GameGenre.ALL, sort, query);
        _edgamesvc.loadArcadeEntryIds(_portal = page, new InfoCallback<int[]> () {
            public void onSuccess (int[] result) {
                _entryIds = Sets.newHashSet();
                for (int gameId : result) {
                    _entryIds.add(gameId);
                }
                for (Integer gameId : _actions.keySet()) {
                    updateAction(gameId);
                }
            }
        });
    }

    @Override
    protected void onSortChanged (Sort sort)
    {
        Link.go(Pages.EDGAMES, "aa", _portal.toByte(), sort.toToken(), _header.getQuery());
    }

    @Override
    protected Widget createActionWidget (GameInfo game)
    {
        FlowPanel action = new FlowPanel();
        _actions.put(game.gameId, action);
        updateAction(game.gameId);
        return action;
    }

    protected void updateAction (final Integer gameId)
    {
        FlowPanel action = _actions.get(gameId);
        if (action == null) {
            return;
        }
        action.clear();
        if (_entryIds == null) {
            action.add(new Label(_msgs.atgLoading()));
            return;
        }
        final boolean topGame = _entryIds.contains(gameId);
        String text = topGame ? _msgs.atgRemove() : _msgs.atgAdd();
        Button button = new Button(text);
        action.add(button);
        new ClickCallback<Void>(button) {
            @Override protected boolean callService () {
                if (topGame) {
                    _edgamesvc.removeArcadeEntry(_portal, gameId, this);
                } else {
                    _edgamesvc.addArcadeEntry(_portal, gameId, this);
                }
                return true;
            }

            @Override protected boolean gotResult (Void result) {
                if (topGame) {
                    _entryIds.remove(gameId);
                } else {
                    _entryIds.add(gameId);
                }
                updateAction(gameId);
                return true;
            }
        };
    }

    protected ArcadeData.Portal _portal;
    protected Set<Integer> _entryIds;
    protected HashMap<Integer, FlowPanel> _actions = Maps.newHashMap();

    protected static final EditGamesMessages _msgs = GWT.create(EditGamesMessages.class);
    protected static final EditGameServiceAsync _edgamesvc = GWT.create(EditGameService.class);
}
