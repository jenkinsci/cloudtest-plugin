package com.soasta.jenkins.cloud.build.StartTestEnvironment;

f=namespace(lib.FormTagLib)

f.entry(title:"CloudTest Server",field:"cloudTestServerID") {
    f.select()
}
f.entry(title:"Test Environment Name",field:"name") {
    f.expandableTextbox()
}


f.advanced {

  f.entry(title:"Seconds to wait until in ready status ",field:"timeOut") {
    f.number()
  }
}