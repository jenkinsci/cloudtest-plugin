/*
 * Copyright (c) 2012, CloudBees, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins.CloudTestServer;

f=namespace(lib.FormTagLib)

f.invisibleEntry {
    f.textbox(field:"id");
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
f.entry(title:"API Token",field:"apitoken") {
    f.textbox()
}


f.advanced {
    f.entry(title:"Full Path to Keystore",field:"keyStoreLocation") {
        f.textbox()
    }
    f.entry(title:"Keystore password",field:"keyStorePassword") {
        f.password()
    }
    f.entry(title:"Trust selfsigned",field:"trustSelfSigned") {
        f.checkbox()
    }
}

f.validateButton(method:"validate",with:"url,username,password,id,name,apitoken,keyStoreLocation,keyStorePassword,trustSelfSigned",title:"Test Connection") 
    f.entry {
        div(align:"right") {
        input(type:"button",value:"Add",class:"repeatable-add")
        input(type:"button",value:"Delete",class:"repeatable-delete")
    }
}


