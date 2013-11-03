/*
 * Copyright (c) 2012, CloudBees, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins.CloudTestServer;

f=namespace(lib.FormTagLib)

f.entry(title:"ID",field:"id") {
    f.readOnlyTextbox();
}
f.entry(title:"Name",field:"name") {
    f.textbox()
}
f.entry(title:"URL",field:"url") {
    f.textbox()
}
f.entry(title:"User Name",field:"username") {
    f.textbox()
}
f.entry(title:"Password",field:"password") {
    f.password()
}
f.validateButton(method:"validate",with:"url,username,password,id,name",title:"Test Connection")
f.entry {
    div(align:"right") {
        input(type:"button",value:"Add",class:"repeatable-add")
        input(type:"button",value:"Delete",class:"repeatable-delete show-if-not-last")
    }
}