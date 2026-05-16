package com.openlab.qualitos.quality.tenantmodules.domain;

public class ModuleActivationNotFoundException extends RuntimeException {
    public ModuleActivationNotFoundException(String module) {
        super("Module not enabled for tenant: " + module);
    }
}
