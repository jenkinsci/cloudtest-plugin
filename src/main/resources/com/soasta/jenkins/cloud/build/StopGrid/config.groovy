package com.soasta.jenkins.StopGrid;

f=namespace(lib.FormTagLib)

f.entry(title:"CloudTest Server",field:"cloudTestServerID") {
    f.select()
}
f.entry(title:"Grid Name",field:"name") {
    f.expandableTextbox()
}

