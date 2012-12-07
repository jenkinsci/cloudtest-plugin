/*
 * Copyright (c) 2012, CloudBees, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins.iOSAppInstaller;

f=namespace(lib.FormTagLib)

if (descriptor.showUrlField()) {
    f.entry(title:"CloudTest Server",field:"url") {
        f.select()
    }
}
f.entry(title:"IPA file",field:"ipa") {
    f.textbox()
}
f.advanced {
    f.entry(title:"Additional Options",field:"additionalOptions") {
        f.expandableTextbox()
    }
}
