package com.soasta.jenkins.TransactionThreshold;

def f = namespace(lib.FormTagLib)
    
f.entry(title:"Transaction Name",field:"transactionname") {
  f.textbox()
}
f.entry(title:"Threshold",field:"thresholdname" ) {
  f.select(name:"thresholdname")
}
f.entry(title:"Threshold Value",field:"thresholdvalue") {
    f.textbox()
  }

f.entry {
  div(align:"left") {
    input(type:"button",value:"Delete",class:"repeatable-delete")
  }
}

