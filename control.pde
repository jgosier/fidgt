//Controls
void mouseReleased(){     
  if(!focused)
    return;
    
  if(mouseOverArt){
    cursor(ARROW);
    return;
  }
  
  if(mouseOverQuery){
    if(draggingMagnet){
      draggingMagnet   = false;  
      draggedMagnet    = null;      
    }
  }
  else{
    draggingMagnet   = false;  
    draggedMagnet    = null;
  }

  if(draggingScreen)  
    draggingScreen   = false;
    
  if(mouseOverDraggable && !draggingDraggable){
    if(hoveringDraggable!=null){
      hoveringDraggable.using = !hoveringDraggable.using;
      
      if(hoveringDraggable == draggableMute)
        sound.toggleMute();
      if(!sound.disableSound)
        sound.itemClickSound.play();            
    }
  }    
  
  /*
  if(draggableFullScreen.mouseOver){
    if(draggableFullScreen.using)
      changeToFullScreen();
    else
      changeToWindowed();
  }*/
  
  if(draggingDraggable)
    draggingDraggable = false;
}//end mouseReleased()

void mousePressed(){
  if(!focused)
    return;  
//  println(mouseX);
//  println(mouseY);

  //manage query mousePressed
  
  if(mouseOverArt){
    cursor(ARROW);        
    if(artLink.equals("") || artLink == null)
      return;      
    if(!sound.disableSound)      
      sound.itemClickSound.play();      
    link(artLink, "gearonAmorphic");
    return;
  }    
  
  if(queryActive && !mouseOverQuery){
    queryActive = false;
    if(!sound.disableSound)    
      sound.itemClickOff.play();
    if(queryString.length()<=0){
      queryString=initialQuery;
      queryTouched = false;     
    }         
  }  
  
  if(mouseOverField){    
    if(selectedField!=null)
      selectedField.active = false;
    if(hoveringField!=null)
      hoveringField.mousePressed();
    if(!sound.disableSound)    
      sound.itemClickSound.play();     
    return;
  }
  else{
    deselectAllFields();
  }
  
  if(mouseOverQuery && queryUsable){
    selectedCell = null;     
    if(!sound.disableSound)    
      sound.itemClickSound.play();
    queryActive = !queryActive;
    if(!queryTouched){
      queryTouched = true;
      queryString = "";
    }    
    if(queryString.length()>0)
      queryAuto = autoComplete(queryString);
    return;
  }
  
  //manage magnet mousePressed
  //deselection
  if(!draggingMagnet && !mouseOverMagnet){
    if(selectedCell != null && !mouseOverCell){
      if(selectedCell.user!=null)
        selectedCell.user.clearImages();
      selectedCell = null;    
    }
    if(!sound.disableSound)
      sound.itemClickOff.play();
  }    
  
  if(mouseOverMagnet && hoveringMagnet!=null){
    if(!hoveringMagnet.mouseOverClose){
      if(!sound.disableSound)      
        sound.itemClickSound.play();     
    }else{
      if(!sound.disableSound)      
        sound.itemClickOff.play();          
    }      
  }

  //manage cell mousePressed
  if(!draggingMagnet){
    for(int i=0;i<cells.size();i++){    
      Cell cell = (Cell)cells.get(i);
      if(cell.hoveringOver){        
        selectCell(cell);  
        break;
      }
    }      
  }
  
  
  
}//end mousePressed()

void mouseDragged(){ 
  if(!focused)
    return;  
  if(mouseOverArt)
    return;
  if(!draggingMagnet && !draggingScreen && mouseOverDraggable){
    draggingDraggable = true;
    draggedDraggable = hoveringDraggable;
  }
}//end mouseDragged()

void keyReleased(){
  for(int i=0; i<magnets.size(); i++){
    Magnet m = (Magnet) magnets.get(i);
    
    //delete a magnet by hovering over it and pressing delete
    if(m.mouseOver && keyCode == 127)
      magnets.remove(i);
  }
}//end keyReleased()

void keyPressed(){
  //println(keyCode);
  
  //hit enter after we've entered a query string
  if(queryActive && keyCode == 10){    
    String testString = queryString;
    if(useAutoString)
      testString = queryAuto;
    
    //don't create a magnet if query string is empty
    if(queryString.length()<=0){
      queryString = initialQuery;
      queryActive = false;
      queryTouched = false;  
      
      //play sound on fail
      if(!sound.disableSound)      
        sound.itemClickOff.play();      
      return;
    }
    
    //don't create a magnet if same magnet already exists
    if(!doesMagnetExist(testString)){
      queryActive = false;      
      
      //success! create the magnet
      createMagnet(testString, queryCustomRadius);    
      
      //do sound for magnet creation
      if(!sound.disableSound)      
        sound.magnetCreate.play();
      
      precomputeStippleEllipse(queryCustomRadius, queryStippleAmount);
      if(useAutoString){
        queryString = queryAuto;
        useAutoString = false; 
        
        //play sound on fail
        if(!sound.disableSound)        
          sound.itemClickOff.play();          
      }
    }
  }else  
  if(queryActive){
    //backspace to delete
    if(keyCode == 8){
      if(queryString.length()>0 && !queryString.equals("") ){
        queryString = queryString.substring(0,queryString.length()-1);  
        queryAuto = autoComplete(queryString);     
        if(!sound.disableSound)        
          sound.magnetClickOff.play();   
      }
      if(queryString.length()==0)
        queryAuto = "";
    }
    else
    //omit these keys
    /*16 38 40 37 39 127*/
    if(keyCode==16 || keyCode==38 || keyCode==40 || keyCode==37 || keyCode==39 || keyCode==127){
      
      //up and down for using/not using autocomplete prompt
      if(keyCode==38 || keyCode==40){
        useAutoString = !useAutoString;
        if(!sound.disableSound)          
          sound.magnetClick.play();
      }
    }
    //any other keys added to query string
    else{
      queryString += key;
      if(!sound.disableSound)      
        sound.magnetClick.play();
      if(queryString.length()>0)
        queryAuto = autoComplete(queryString);
      else
        queryAuto = "";
    }
  }else
  
    if(selectedField!=null)
      selectedField.keyPressed();
  
  //handle user search box
  /*
  if(usingSearch){
    //pressed enter
    if(keyCode == 10){
      if(usingSearchAuto){
        //autocompleted
        usingSearchAuto = false;
        userSearchString = userSearchAutoString;
      }
      //regular search
      executeSearch(userSearchString);
      usingSearch = false;
      return;
    }
    
    //backspace to delete
    if(keyCode == 8){
      if(userSearchString.length()>0 && !userSearchString.equals("") ){
        userSearchString = userSearchString.substring(0,userSearchString.length()-1);  
        userSearchAutoString = autoCompleteSearch(userSearchString);     
        if(!sound.disableSound)        
          sound.magnetClickOff.play();   
      }
      if(userSearchString.length()==0)
        userSearchString = "";
    }
    else    
    if(keyCode==16 || keyCode==38 || keyCode==40 || keyCode==37 || keyCode==39 || keyCode==127){
      
      //up and down for using/not using autocomplete prompt
      if(keyCode==38 || keyCode==40){
        usingSearchAuto = !usingSearchAuto;
        if(!sound.disableSound)        
          sound.magnetClick.play();
      }
    }
    //any other keys added to field string
    else{
      userSearchString += key;
      if(!sound.disableSound)      
        sound.magnetClick.play();
      if(userSearchString.length()>0)
        userSearchAutoString = autoCompleteSearch(userSearchString);
      else
        userSearchAutoString = "";
    }    
  }
  */  
  
  if(key==ESC){
    queryActive = false;
    deselectAllFields();
    //usingSearch = false;
    
    //play sound on fail
    if(!sound.disableSound)    
      sound.itemClickOff.play();     
    
    if(queryString.length()<=0){
      queryString=initialQuery;
      queryTouched = false;     
    }     
    //prevent program exit!    
    key=0;
  }
  
  if(!queryActive && !usingTextField){
    if(key == 's')
      saveFrame("amorphic_ss-####.tif");
  }
  
  //if(key == 'u')
  //  displayUserLog = !displayUserLog;
}//end keyPressed()
