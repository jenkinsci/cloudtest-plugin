package com.soasta.jenkins.CloudTestServer;

f=namespace(lib.FormTagLib)

f.entry(title:"URL",field:"url") {
    f.textbox()
}
f.entry(title:"User Name",field:"username") {
    f.textbox()
}
f.entry(title:"Password",field:"password") {
    f.password()
}
f.validateButton(method:"validate",with:"url,username,password",title:"Test Connection")
f.entry {
    div(align:"right") {
        input(type:"button",value:"Add",class:"repeatable-add")
        input(type:"button",value:"Delete",class:"repeatable-delete show-if-not-last")
    }
}