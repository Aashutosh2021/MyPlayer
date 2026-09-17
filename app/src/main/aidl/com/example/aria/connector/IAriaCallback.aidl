package com.example.aria.connector;

import com.example.aria.connector.EcosystemEventContainer;
import com.example.aria.connector.Capability;

oneway interface IAriaCallback {
    void onEvent(in EcosystemEventContainer event);
    void onCapabilitiesChanged(in List<Capability> capabilities);
}
