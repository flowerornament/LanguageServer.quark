// Base Document class providing the common interface for document representations.
// LSPDocument subclass implements this for LSP clients.

Document {
    classvar <dir="", <allDocuments, <>current;
    classvar <globalKeyDownAction, <globalKeyUpAction, <>initAction;
    classvar <>autoRun = true;
    classvar <asyncActions;
    classvar <>implementingClass;
    
    var <>quuid, <title, <isEdited = false;
    var <>toFrontAction, <>endFrontAction, <>onClose, <>textChangedAction;
    
    var <envir, <savedEnvir;
    // var <editable = true, <promptToSave = true;
    
    path            { ^this.subclassResponsibility(thisMethod) }
    keyDownAction   { ^this.subclassResponsibility(thisMethod) }
    keyDownAction_  { ^this.subclassResponsibility(thisMethod) }
    keyUpAction     { ^this.subclassResponsibility(thisMethod) }
    keyUpAction_    { ^this.subclassResponsibility(thisMethod) }
    mouseUpAction   { ^this.subclassResponsibility(thisMethod) }
    mouseUpAction_  { ^this.subclassResponsibility(thisMethod) }
    mouseDownAction { ^this.subclassResponsibility(thisMethod) }
    mouseDownAction_{ ^this.subclassResponsibility(thisMethod) }
    
    *open { 
        |path, selectionStart=0, selectionLength=0, envir| 
        ^implementingClass.open(path, selectionStart=0, selectionLength=0, envir) 
    }
    
    open { 
        |path, selectionStart=0, selectionLength=0, envir| 
        ^implementingClass.open(path, selectionStart=0, selectionLength=0, envir) 
    }
    
    *implementationClass { ^LSPDocument }
}
