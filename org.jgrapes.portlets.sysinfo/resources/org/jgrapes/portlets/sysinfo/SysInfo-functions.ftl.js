'use strict';

var orgJGrapesPortletsSysInfo = {
    };

(function() {

    let maxMemoryData = [];
    let totalMemoryData = [];
    let freeMemoryData = [];
    for (let i = -300; i <= 0; i++) {
        maxMemoryData.push(NaN);
        totalMemoryData.push(NaN);
        freeMemoryData.push(NaN);
    }
    
    JGPortal.registerPortletMethod(
            "org.jgrapes.portlets.sysinfo.SysInfoPortlet",
            "updateMemorySizes", function(portletId, params) {
                maxMemoryData.shift();
                maxMemoryData.push(params[0]);
                totalMemoryData.shift();
                totalMemoryData.push(params[1]);
                freeMemoryData.shift();
                freeMemoryData.push(params[2]);
                let maxFormatted = "";
                let totalFormatted = "";
                let freeFormatted = "";
                let portlet = JGPortal.findPortletPreview(portletId);
                if (portlet) {
                    let lang = portlet.closest('[lang]').attr('lang') || 'en'
                    maxFormatted = JGPortal.formatMemorySize(params[0], 1, lang);
                    totalFormatted = JGPortal.formatMemorySize(params[1], 1, lang);
                    freeFormatted = JGPortal.formatMemorySize(params[2], 1, lang);
                    let col = portlet.find(".maxMemory");
                    col.html(maxFormatted);
                    col = portlet.find(".totalMemory");
                    col.html(totalFormatted);
                    col = portlet.find(".freeMemory");
                    col.html(freeFormatted);
                }
                portlet = JGPortal.findPortletView(portletId);
                if (portlet) {
                    let col = portlet.find(".maxMemory");
                    col.html(maxFormatted);
                    col = portlet.find(".totalMemory");
                    col.html(totalFormatted);
                    col = portlet.find(".freeMemory");
                    col.html(freeFormatted);
                    let chartCanvas = portlet.find(".memoryChart");
                    if (portlet.find(".memoryChart").parent(":hidden").length === 0) {
                        let chart = chartCanvas.data('chartjs-chart');
                        chart.update(0);
                    }
                }
            });

    orgJGrapesPortletsSysInfo.initMemoryChart = function(chartCanvas) {
        var ctx = chartCanvas[0].getContext('2d');
        let labels = [];
        for (let i = -300; i <= 0; i++) {
            labels.push(i + "s");
        }
        let lang = chartCanvas.closest('[lang]').attr('lang') || 'en'
        var chart = new Chart(ctx, {
            // The type of chart we want to create
            type: 'line',

            // The data for our datasets
            data: {
                labels: labels,
                datasets: [{
                    lineTension: 0,
                    fill: false,
                    pointRadius: 0,
                    borderColor: "rgba(255,0,0,1)",
                    label: "${_("maxMemory")}",
                    data: maxMemoryData,
                },{
                    lineTension: 0,
                    fill: false,
                    pointRadius: 0,
                    borderColor: "rgba(255,165,0,1)",
                    label: "${_("totalMemory")}",
                    data: totalMemoryData,
                },{
                    lineTension: 0,
                    fill: false,
                    pointRadius: 0,
                    borderColor: "rgba(0,255,0,1)",
                    label: "${_("freeMemory")}",
                    data: freeMemoryData,
                }]
            },

            // Configuration options go here
            options: {
                maintainAspectRatio: false,
                scales: {
                    yAxes: [{
                        ticks: {
                            callback: function(value, index, values) {
                                return JGPortal.formatMemorySize(value, 0, lang);
                            }
                        }
                    }]
                }
            }
        });
        chartCanvas.data('chartjs-chart', chart);
    }
    
})();

