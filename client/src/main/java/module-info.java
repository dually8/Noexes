module JNoexsClient {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.prefs;
    requires usb.api;
    requires Utils;
    requires java.desktop;
    requires gson;
    requires usb4java.javax;
    requires usb4java;

    opens me.mdbell.noexs.ui to javafx.fxml;
    exports me.mdbell.noexs.ui;
}