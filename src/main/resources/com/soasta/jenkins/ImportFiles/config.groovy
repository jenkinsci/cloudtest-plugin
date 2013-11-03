package com.soasta.jenkins.ImportFiles;

f=namespace(lib.FormTagLib)

f.entry(title:"CloudTest Server",field:"cloudTestServerID") {
    f.select()
}
f.entry(title:"File(s) to import",field:"files") {
    f.expandableTextbox()
}
f.entry(title:"Conflict resolution mode",field:"mode") {
    f.select(name:"mode")
}
f.advanced {
    f.entry(title:"Excludes",field:"excludes") {
        f.textbox()
    }
    f.entry(title:"Additional Options",field:"additionalOptions") {
        f.expandableTextbox()
    }
}
