if (!whirled) {
    var whirled = {};
}

whirled.addCharts = function () {
    eval(bedrock.include({
            'bedrock.util': ['log'],
            'bedrock.iter': ['each'],
            'bedrock.collections': ['List','Dict'],
            'panopticon.chart': ['addChart', 'init', 'StackedBarChart',
                                 'SelfContainedEventChart'],
            'panopticon.ui': ['CheckBoxes', 'RadioButtons'],
        }));

    return function() {
        addChart("economy", "exchange_rate", "Exchange Rate", function () {
            var options = {
                xaxis: { mode: "time", minTickSize: [1, "day"]}
            };
            var chart = new SelfContainedEventChart(
                "DailyExchangeRate", function (ev, collector) {
                    collector.assume("Exchange Rate").add(
                        [ev.date, ev.rate]);
                }, options);
            return chart;
        });

        addChart("economy", "Earnings", "Earnings", function () {
            var actionNames = new List([
                [20, "Games"], [31, "Purchases"], [34, "Payouts"], [40, "Badges"],
                [50, "Bars Purchased"], [51, "Payouts"], [54, "Bling to Bars"],
                [55, "Cashed Out"]
            ]);
            var actions = new CheckBoxes("Actions", "actions", actionNames);
            var currency = new RadioButtons("Currency", "currency", ["coins", "bar", "bling" ]);
            function valueExtractor (event, name) {
                return (currency.value == event.currency && actions.has(event.actionType) ?
                        (event[earned] || 0) : 0;
            }
            return new StackedBarChart(
                "DailyTransactions", actionNames, valueExtractor, {controls:[actions]});
        });

        addChart("funnel", "funnel_web", "Conversion and Retention", function () {
            var sourceNames = new List([
                ["lost", "Lost"],
                ["played", "Played"],
                ["converted", "Converted"],
                ["returned", "Returned"],
                ["retained", "Retained"]
            ]);
            var sources = new CheckBoxes("Sources", "sources", sourceNames);
            var group = new RadioButtons("Group", "group", ["web", "embed" ]);
            function valueExtractor (event, name) {
                return (group.value == event.group && sources.has(name)) ?
                    (event[name] || 0) : 0;
            }
            var chart = new StackedBarChart(
                "DailyVisitorFutures", sourceNames, valueExtractor, {controls:[sources, group]});
            chart.extractKey = function (ev) { return ev.date; }
            return chart;
        });
    }
}();
