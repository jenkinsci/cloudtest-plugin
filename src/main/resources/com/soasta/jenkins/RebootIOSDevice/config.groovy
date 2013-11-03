/*
 * Copyright (c) 2013, SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins.RebootIOSDevice;

f=namespace(lib.FormTagLib)

f.entry(title:"CloudTest Server",field:"cloudTestServerID") {
    f.select()
}
f.advanced {
    f.entry(title:"Additional Options",field:"additionalOptions") {
        f.expandableTextbox()
    }
}
