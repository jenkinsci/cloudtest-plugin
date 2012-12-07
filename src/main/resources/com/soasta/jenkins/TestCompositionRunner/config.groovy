package com.soasta.jenkins.TestCompositionRunner;

f=namespace(lib.FormTagLib)

if (descriptor.showUrlField()) {
    f.entry(title:"CloudTest Server",field:"url") {
        f.select()
    }
}
f.entry(title:"Composition",field:"composition") {
    f.textbox()
}
