//
// $Id$

package client.people;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.orth.data.MediaDescSize;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.game.gwt.GameRating;
import com.threerings.msoy.profile.gwt.ProfileService;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.util.Link;
import client.util.MediaUtil;
import client.util.NaviUtil;

/**
 * Displays a person's game ratings.
 */
public class RatingsBlurb extends Blurb
{
    @Override // from Blurb
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return (pdata.ratings != null && pdata.ratings.size() > 0);
    }

    @Override // from Blurb
    public void init (ProfileService.ProfileResult pdata)
    {
        super.init(pdata);
        setHeader(_msgs.ratingsTitle());
        setContent(new RatingGrid(pdata.ratings));
    }

    protected class RatingGrid extends PagedGrid<GameRating>
    {
        public RatingGrid (List<GameRating> ratings)
        {
            super(RATING_ROWS, 2, NAV_ON_BOTTOM);
            setModel(new SimpleDataModel<GameRating>(ratings), 0);
        }

        @Override // from PagedGrid
        protected String getEmptyMessage ()
        {
            return ""; // not used
        }

        @Override // from PagedGrid
        protected boolean displayNavi (int items)
        {
            return (items > _rows * _cols);
        }

        @Override // from PagedGrid
        protected Widget createWidget (GameRating rating)
        {
            return new RatingWidget(rating);
        }
    }

    protected class RatingWidget extends FlexTable
    {
        public RatingWidget (final GameRating entry)
        {
            setCellPadding(0);
            setCellSpacing(0);
            setStyleName("ratingWidget");
            getFlexCellFormatter().setStyleName(0, 0, "GameThumb");
            getFlexCellFormatter().setStyleName(0, 1, "GameName");

            ClickHandler gameClick = new ClickHandler() {
                public void onClick (ClickEvent event) {
                    Link.go(Pages.GAMES, NaviUtil.GameDetails.MYRANKINGS.args(entry.gameId));
                }
            };
            setWidget(0, 0, MediaUtil.createMediaView(
                          entry.gameThumb, MediaDescSize.HALF_THUMBNAIL_SIZE, gameClick));
            if (entry.singleRating > 0) {
                getFlexCellFormatter().setRowSpan(0, 0, 2);
            }

            setWidget(0, 1, MsoyUI.createActionLabel(entry.gameName, gameClick));

            if (entry.multiRating > 0) {
                setText(0, 2, "" + entry.multiRating);
                getFlexCellFormatter().setStyleName(0, 2, "Rating");
            }

            if (entry.singleRating > 0) {
                setText(1, 0, _msgs.ratingsSingle());
                getFlexCellFormatter().setStyleName(1, 0, "Note");
                setText(1, 1, "" + entry.singleRating);
                getFlexCellFormatter().setStyleName(1, 1, "Rating");
            }
        }
    }

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);

    protected static final int RATING_ROWS = 2;
}
