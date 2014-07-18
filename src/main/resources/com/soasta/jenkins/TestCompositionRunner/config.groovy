package com.soasta.jenkins.TestCompositionRunner;

f=namespace(lib.FormTagLib)

f.entry(title:"CloudTest Server",field:"cloudTestServerID") {
    f.select()
}
f.entry(title:"Composition(s)",field:"composition") {
    f.expandableTextbox()
}

f.advanced {

  f.optionalBlock(title:"Delete old results from the CloudTest server",field:"deleteOldResults") {
    f.entry(title:"Days to keep results",field:"maxDaysOfResults") {
      f.number()
    }
  }

  f.entry(title:"Additional Options",field:"additionalOptions") {
    f.expandableTextbox()
  }

  f.entry(title:"Transaction Thresholds") {
    f.repeatableProperty(field:"thresholds")
  }

  f.entry(title:"Generate CSV for Plot Plugin",field:"generatePlotCSV") {
      f.checkbox()
  }
}
