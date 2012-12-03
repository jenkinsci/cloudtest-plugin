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
f.block {
    f.validateButton(method:"validate",with:"url,username,password",title:"Test Connection")
}