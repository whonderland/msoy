//
// $Id$

package client.shop;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.gwt.CatalogQuery;

import client.shell.CShell;
import client.shell.DynamicLookup;
import client.item.ShopUtil;
import client.item.SideBar;
import client.item.TagCloud;
import client.shell.Pages;
import client.ui.Marquee;
import client.util.Link;

/**
 * Displays a tabbed panel containing the catalog.
 */
public class CatalogPanel extends SmartTable
    implements TagCloud.TagListener
{
    public CatalogPanel (CatalogModels models)
    {
        super("catalogPanel", 0, 0);
        _models = models;

        // create our listings interface
        _listings = new SmartTable("Listings", 0, 0);
        // the blurb and type will be set into (0, 0) and (0, 1) later
        _listings.getFlexCellFormatter().setRowSpan(0, 0, 2);

        getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
        setWidget(0, 1, WidgetUtil.makeShim(10, 10));
        setWidget(0, 2, _listings, 1, "ListingsCell");
        getFlexCellFormatter().setVerticalAlignment(0, 2, HasAlignment.ALIGN_TOP);
        setWidget(0, 3, WidgetUtil.makeShim(10, 10));

        _searchBox = new TextBox();
        _searchBox.setVisibleLength(20);
        _searchBox.addStyleName("itemSearchBox");
        ClickListener clickListener = new ClickListener() {
            public void onClick (Widget sender) {
                String query = _searchBox.getText().trim();
                Link.go(Pages.SHOP, ShopUtil.composeArgs(_query, null, query, 0));
            }
        };
        _searchBox.addKeyboardListener(new EnterClickAdapter(clickListener));

        Button searchGo = new Button(_msgs.catalogSearch());
        searchGo.addClickListener(clickListener);

        HorizontalPanel search = new HorizontalPanel();
        search.add(_searchBox);
        search.add(WidgetUtil.makeShim(5, 5));
        search.add(searchGo);
        _listings.setWidget(1, 0, search);

        _sortBox = new ListBox();
        for (String label : SORT_LABELS) {
            _sortBox.addItem(label);
        }
        _sortBox.addChangeListener(new ChangeListener() {
            public void onChange (Widget widget) {
                _query.sortBy = SORT_VALUES[((ListBox)widget).getSelectedIndex()];
                Link.go(Pages.SHOP, ShopUtil.composeArgs(_query, 0));
            }
        });

        _items = new ListingGrid(HEADER_HEIGHT) {
            @Override protected void displayPageFromClick (int page) {
                Link.go(Pages.SHOP, ShopUtil.composeArgs(_query, page));
            }
            @Override protected String getEmptyMessage () {
                String name = _dmsgs.xlate("itemType" + _query.itemType);
                if (_query.tag != null) {
                    return _msgs.catalogNoTag(name, _query.tag);
                } else if (_query.search != null) {
                    return _msgs.catalogNoMatch(name, _query.search);
                } else if (_query.creatorId != 0) {
                    return _msgs.catalogNoCreator(name);
                } else {
                    return _msgs.catalogNoList(name);
                }
            }
            @Override protected void configureNavi (
                FlexTable controls, int row, int col, int start, int limit, int total) {
                super.configureNavi(controls, row, col, start, limit, total);
                controls.getFlexCellFormatter().setHorizontalAlignment(
                    row, col, HasAlignment.ALIGN_RIGHT);
            }
            @Override protected void addCustomControls (FlexTable controls) {
                controls.setWidget(
                    0, 0, new InlineLabel(_msgs.catalogSortBy(), false, false, false));
                controls.getFlexCellFormatter().setStyleName(0, 0, "SortBy");
                controls.setWidget(0, 1, _sortBox);
            }
        };
        _listings.setWidget(2, 0, _items, 2, null);
        _listings.getFlexCellFormatter().setHeight(2, 0, "100%");
        _listings.getFlexCellFormatter().setVerticalAlignment(2, 0, HasAlignment.ALIGN_TOP);
    }

    public void display (CatalogQuery query, int pageNo)
    {
        _query = query;

        String tname = _dmsgs.xlate("pItemType" + _query.itemType);
        // TODO: add logo image
        _listings.setWidget(0, 1, new Marquee(null, tname));

        // configure our filter interface
        _searchBox.setText(_query.search == null ? "" : _query.search);
        for (int ii = 0; ii < SORT_VALUES.length; ii++) {
            if (SORT_VALUES[ii] == _query.sortBy) {
                _sortBox.setSelectedIndex(ii);
            }
        }

        if (_query.search != null) {
            setFilteredBy(_msgs.catalogSearchFilter(_query.search));
        } else if (_query.tag != null) {
            setFilteredBy(_msgs.catalogTagFilter(_query.tag));
        } else if (_query.creatorId != 0) {
            setFilteredBy(_msgs.catalogCreatorFilter());
        } else {
            setFilteredBy(null);
        }

        // grab our data model and display it
        CatalogModels.Listings model = _models.getListingsModel(_query);
        CatalogModels.Listings current = (CatalogModels.Listings)_items.getModel();
        if (current != null && current.getType() != model.getType()) {
            // clear the display when we switching item types so that we don't see items of the
            // old type while items of the new type are loading
            _items.clear();
        }
        _items.setModel(model, pageNo);

        // configure the appropriate tag cloud
        TagCloud cloud = _clouds.get(_query.itemType);
        if (cloud == null) {
            _clouds.put(_query.itemType, cloud = new TagCloud(_query.itemType, TAG_COUNT, this));
        }
        setWidget(0, 0, new SideBar(new CatalogQueryLinker(_query), false, cloud));

        // set up our page title
        CShell.frame.setTitle(_dmsgs.xlate("pItemType" + _query.itemType));
    }

    // from interface TagCloud.TagListener
    public void tagClicked (String tag)
    {
        Link.go(Pages.SHOP, ShopUtil.composeArgs(_query, tag, null, 0));
    }

    protected void setFilteredBy (String text)
    {
        if (text == null) {
            String blurb = _dmsgs.xlate("catIntro" + _query.itemType);
            _listings.setText(0, 0, blurb, 1, "Blurb");

        } else {
            FlowPanel filter = new FlowPanel();
            filter.add(new InlineLabel(text, false, false, true));
            CatalogQuery query = new CatalogQuery();
            query.itemType = _query.itemType;
            query.sortBy = _query.sortBy;
            filter.add(Link.create(_msgs.catalogClearFilter(),
                                              Pages.SHOP, ShopUtil.composeArgs(query, 0)));
            _listings.setWidget(0, 0, filter, 1, "Blurb");
        }
    }

    protected CatalogQuery _query;
    protected CatalogModels _models;
    protected Map<Byte, TagCloud> _clouds = new HashMap<Byte, TagCloud>();

    protected SmartTable _listings;
    protected TextBox _searchBox;
    protected ListBox _sortBox;
    protected ListingGrid _items;

    protected static final ShopMessages _msgs = GWT.create(ShopMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);

    protected static final int TAG_COUNT = 10;

    // TODO: this looks out of date
    protected static final int HEADER_HEIGHT = 15 /* gap */ + 59 /* top tags, etc. */;

    protected static final String[] SORT_LABELS = new String[] {
        _msgs.sortByNewAndHot(),
        _msgs.sortByRating(),
        _msgs.sortByListDate(),
        _msgs.sortByPriceAsc(),
        _msgs.sortByPriceDesc(),
        _msgs.sortByPurchases(),
        _msgs.sortByFavorites(),
    };
    protected static final byte[] SORT_VALUES = new byte[] {
        CatalogQuery.SORT_BY_NEW_AND_HOT,
        CatalogQuery.SORT_BY_RATING,
        CatalogQuery.SORT_BY_LIST_DATE,
        CatalogQuery.SORT_BY_PRICE_ASC,
        CatalogQuery.SORT_BY_PRICE_DESC,
        CatalogQuery.SORT_BY_PURCHASES,
        CatalogQuery.SORT_BY_FAVORITES,
    };
}
