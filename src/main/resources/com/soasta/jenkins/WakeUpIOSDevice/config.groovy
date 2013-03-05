/*
 * Copyright (c) 2013, SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins.WakeUpIOSDevice;

f=namespace(lib.FormTagLib)

if (descriptor.showUrlField()) {
    f.entry(title:"CloudTest Server",field:"url") {
        f.select()
    }
}
f.advanced {
    f.entry(title:"Additional Options",field:"additionalOptions") {
        f.expandableTextbox()
    }
}
