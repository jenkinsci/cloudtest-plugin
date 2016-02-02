package com.soasta.jenkins.StartTestEnvironment;

f=namespace(lib.FormTagLib)

f.entry(title:"CloudTest Server",field:"cloudTestServerID") {
    f.select()
}
f.entry(title:"Test Environment Name",field:"name") {
    f.expandableTextbox()
}
f.entry(title:"CTM User Name",field:"ctmUserName") {
    f.expandableTextbox()
}
f.entry(title:"CTM User Password",field:"ctmPassword") {
    f.password()
}


