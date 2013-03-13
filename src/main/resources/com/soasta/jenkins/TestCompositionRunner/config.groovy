package com.soasta.jenkins.TestCompositionRunner;

f=namespace(lib.FormTagLib)

if (descriptor.showUrlField()) {
    f.entry(title:"CloudTest Server",field:"url") {
        f.select()
    }
}
f.entry(title:"Composition(s)",field:"composition") {
    f.expandableTextbox()
}
f.advanced {
    f.block() {
        table() {
            f.optionalBlock(title:"Delete old results from the CloudTest server",field:"deleteOldResults") {
                f.entry(title:"Days to keep results",field:"maxDaysOfResults") {
                    f.number()
                }
            }
        }
    }
}
