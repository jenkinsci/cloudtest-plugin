package com.soasta.jenkins.StopTestEnvironment;

f=namespace(lib.FormTagLib)


f.entry(title:"CTM URL",field:"url") {
    f.expandableTextbox()
}
f.entry(title:"CloudTest Server",field:"cloudTestServerID") {
    f.select()
}
f.entry(title:"Test Environment Name",field:"name") {
    f.expandableTextbox()
}

