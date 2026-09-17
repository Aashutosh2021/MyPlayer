package com.example.aria.connector;

import com.example.aria.connector.IAriaCallback;
import com.example.aria.connector.AppCommandContainer;
import com.example.aria.connector.AppResponseContainer;
import com.example.aria.connector.ConnectorVersionInfo;
import com.example.aria.connector.Capability;

interface IAriaConnector {
    AppResponseContainer execute(in AppCommandContainer command);
    List<Capability> getCapabilities();
    ConnectorVersionInfo getVersionInfo();
    String getAppName();
    void registerCallback(IAriaCallback callback);
    void unregisterCallback(IAriaCallback callback);

    // Fallback for older legacy JSON-based connectors to maintain backward compatibility
    String executeLegacy(String commandJson);
}
