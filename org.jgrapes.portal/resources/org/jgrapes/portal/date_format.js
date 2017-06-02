// Date formatting 
// (based on http://jacwright.com/projects/javascript/date_format/)
Date.prototype.toString = function(format) {
    var returnStr = '';
    var replace = Date.replaceChars;
    for (var i = 0; i < format.length; i++) {       
        var curChar = format.charAt(i);
        if (i - 1 >= 0 && format.charAt(i - 1) == "\\") {
            returnStr += curChar;
        } else if (replace[curChar]) {
            key = curChar;
            while(i + 1 < format.length && format.charAt(i+1) == curChar) {
                key = key + curChar;
                i = i + 1;
            }
            if (replace[key]) {
                returnStr += replace[key].call(this);
            } else {
                returnStr += "(invalid format)";
            }
        } else if (curChar != "\\"){
            returnStr += curChar;
        }
    }
    return returnStr;
};

Date.replaceChars = {
    // Day
    d: function() { return this.getDate(); },
    dd: function() { return (this.getDate() < 10 ? '0' : '') + this.getDate(); },
    ddd: function() { return Date.replaceChars.shortDays[this.getDay()]; },
    dddd: function() { return Date.replaceChars.longDays[this.getDay()]; },
    // Month
    M: function() { return this.getMonth() + 1; },
    MM: function() { return (this.getMonth() < 9 ? '0' : '') + (this.getMonth() + 1); },
    MMM: function() { return Date.replaceChars.shortMonths[this.getMonth()]; },
    MMMM: function() { return Date.replaceChars.longMonths[this.getMonth()]; },
    // Year
    y: function() { return ''; }, // required for algorithm
    yy: function() { return ('' + this.getFullYear()).substr(2); },
    yyyy: function() { return this.getFullYear(); },
    // Time
    t: function() { return ''; }, // required for algorithm
    tt: function() { return this.getHours() < 12 ? 'AM' : 'PM'; },
    h: function() { return this.getHours() % 12 || 12; },
    H: function() { return this.getHours(); },
    hh: function() { return ((this.getHours() % 12 || 12) < 10 ? '0' : '') + (this.getHours() % 12 || 12); },
    HH: function() { return (this.getHours() < 10 ? '0' : '') + this.getHours(); },
    m: function() { return this.getMinutes(); },
    mm: function() { return (this.getMinutes() < 10 ? '0' : '') + this.getMinutes(); },
    s: function() { return this.getSeconds(); },
    ss: function() { return (this.getSeconds() < 10 ? '0' : '') + this.getSeconds(); },
    // Date
    D: function() { return this.toString(Date.replaceChars.shortDate); },
    DDDD: function() { return this.toString(Date.replaceChars.longDate); },
    // Time
    T: function() { return this.toString(Date.replaceChars.shortTime); },
    TTTT: function() { return this.toString(Date.replaceChars.longTime); },
    // Full
    F: function() { return this.toString(Date.replaceChars.shortDateTime); },
    FFFF: function() { return this.toString(Date.replaceChars.longDateTime); },
};

