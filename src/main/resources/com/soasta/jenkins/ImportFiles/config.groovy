package com.soasta.jenkins.ImportFiles;

f=namespace(lib.FormTagLib)

if (descriptor.showUrlField()) {
    f.entry(title:"CloudTest Server",field:"url") {
        f.select()
    }
}
f.entry(title:"File(s) to import",field:"files") {
    f.textbox()
}
f.advanced {
    f.entry(title:"Excludes",field:"excludes") {
        f.textbox()
    }
}
