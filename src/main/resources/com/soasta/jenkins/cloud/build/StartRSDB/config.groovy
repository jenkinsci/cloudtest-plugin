package com.soasta.jenkins.StartRSDB;

f=namespace(lib.FormTagLib)

f.entry(title:"CloudTest Server",field:"cloudTestServerID") {
    f.select()
}
f.entry(title:"RSDB Name",field:"name") {
    f.expandableTextbox()
}

f.advanced {

  f.entry(title:"Seconds to wait until in ready status ",field:"timeOut") {
    f.number(value:1200)
  }
}
