ArrayList     cells             = new ArrayList(500);
int        numberOfCells     = 300;
Cell       selectedCell;
Cell       hoveringCell;
boolean    mouseOverCell     = false;
boolean    aCellSelected     = false;
float      springConstant    = .15;
float      maxCellRadius     = 30;
float      eggSize           = 6;
 
void createCellsFromUsers(){
  for(int i=0;i<users.size();i++){
    User user = (User) users.get(i);
    float cellRadius = 3;
    if(random(100)>99)
      cellRadius = random(12,40);
    else
    if(random(100)>90)
      cellRadius = random(5,12);
    else
      cellRadius = random(1,5);
    int randomDegree = int(random(0,3));    
    Cell cell = new Cell( user, new Vec2(random(width), random(height) ), cellRadius, 6 );    
    user.cell = cell;    
    cells.add(cell);
  }    
} 

Cell createCellFromUser(User user){
  if(user == null)
    return null;
  
  float cellRadius = eggSize;
  /*
  if(random(100)>99)
    cellRadius = random(12,40);
  else
  if(random(100)>90)
    cellRadius = random(5,12);
  else
    cellRadius = random(1,5);
  int randomDegree = int(random(0,3));    */
  
  if(!sound.disableSound)
    sound.userPopEgg.play();
  Cell cell = new Cell( user, new Vec2(random(width), random(height) ), cellRadius, 6 );    
  user.cell = cell;  
  cells.add(cell); 
  return cell;
}
  
void generateRandomUsers(){
  for(int i=0;i<numberOfCells;i++){
    float cellRadius = 3;
    if(random(100)>99)
      cellRadius = random(12,40);
    else
    if(random(100)>90)
      cellRadius = random(5,12);
    else
      cellRadius = random(1,5);
    int randomDegree = int(random(0,3));    
    Cell cell = new Cell( new User(), new Vec2(random(width), random(height) ), cellRadius, 6 );    
    cells.add(cell);
  }  
}  
  
class Cell{
  //Vec2 pos;
  mass m;
  float weight = .1;
  SubCell[] subs;
  float radius;
  boolean hoveringOver = false;
  boolean lastHoverOver = false;
  boolean isSelected = false;
  boolean revealMagnet = false;
  boolean inHistoryLog = false;
  
  boolean smallCell = false;  
  
  float spinnerAngle = 0;
  
  //egg status is a cell that hasn't recieved user data
  boolean egg = true;
  
  User user;
  
  Vec2 historyPos = new Vec2();
  Vec2 historyAnchor = new Vec2();
  
//  MusicTaste taste;
  
  color cellColorSet = #FFFFFF;
  color cellColor = #000000;
  color cellMagnetColor = #000000;
  static final color eggColor = #849508;
  static final color eggOuterColor = #8EFA47;
  
  float cellHistoryLineOpa = 255;  
  
  boolean leaveTrail = false;     
  
  boolean wandering = true;
  float wanderDistance;
  float wanderSpeed;
  float orientation;
  boolean ignoreCollisions = false;
  
  float sinWave = 0;
  
  boolean isFriendSpawn = false;
  
  Cell(User user, Vec2 pos, float radius, int sides){    
    this.user = user;
    
    weight = ( weight * radius) / 50;
    this.radius = radius;
    
    m = new mass(pos);
    if(sides<6){
      println("debug: attempted to make cell with less than six sides");      
      sides = 6;
    }           
    
    if(radius<=7){
      sides = 0;
      this.radius *=2;
      smallCell = true;
    }
    subs = new SubCell[sides];           
 
    if(!smallCell)   
      giveCellSides(sides);    
      
    orientation = random(-360,360);
    wanderDistance = random(queryRadius * 2, 450);
    wanderSpeed = random(.00001, .0004);
    
    sinWave = random(-100,100);
  }
  
  void giveCellSides(int sides){
    if(sides!=0){
      float angle=0;
      float angleDelta = 360/(sides);
      
      if(sides<6)
        sides=6;
            
      for(int i=0; i<sides; i++){      
         Vec2 subPos = newVec(this.m.p, angle, radius);
         subs[i] = new SubCell(subPos);
         angle+=angleDelta;       
      }
    
      for(int i=0; i<sides; i++){
        m.connectSpringTo(subs[i].m, springConstant*3);      
        for(int u=0; u<sides; u++){
          if(i!=u)        
            subs[i].connectWith(subs[u]);
        }
      }      
    }    
  }
  
  void startFromField( TextField field ){
    m = new mass(field.fieldX, field.fieldY);   
    Vec2 velocity = new Vec2(m.p, queryPos);
    
    //how much to shoot to the center
    float scaleAspectX = random(-.1,.4);
    float scaleAspectY = random(-.1,.4);    
    
    velocity.mul(scaleAspectX, scaleAspectY);
    m.v.nowEquals(velocity); 
  }
  
  void startFromCell( Cell cell ){
    m = new mass(cell.m.p);      
    
    //how much to shoot to the center
    float scaleAspectX = random(-.4,.4);
    float scaleAspectY = random(-.4,.4);    
    Vec2 velocity = new Vec2(scaleAspectX, scaleAspectY);
    velocity.mul(scaleAspectX, scaleAspectY);
    m.v.nowEquals(velocity); 
  }  
  
  void friendSpawn(){
    isFriendSpawn = true;
  }
    
  void rebuild(){    
    //some equation to build radius
    radius =  14;
    radius += constrain((user.taste.numberOftags() / 6),0,40);
    radius += (user.taste.photos / 200);
    radius += (user.taste.playcount * user.taste.playcount) / 200000000;
    radius /= 2;
    
    weight = ( weight * radius) / 25;
    egg = false;    
    int sides = 6;
    if(radius<=7){
      sides = 0;
      smallCell = true;
    }
    else{
      if(radius > maxCellRadius)
        radius = maxCellRadius;
      smallCell = false;
      subs = new SubCell[sides];              
      giveCellSides(sides);      
    }
    if(!sound.disableSound)    
      sound.userPop.play();

  }
  
  void update(){
    
    if(egg)
      sinWave+=.1;
        
    try{
      
    if(selectedCell == this)
      isSelected = true;
    else
      isSelected = false;
      
    if(dist(mp,this.m.p)<radius){
      hoveringOver = true;    
      wandering = false;
      ignoreCollisions = true;
    }
    else{
      hoveringOver = false;
      ignoreCollisions = false;
    }
    
    revealMagnet = false;
    if((mouseOverMagnet || draggingMagnet) && hoveringMagnet!=null){
      CellAttraction ca = (CellAttraction) hoveringMagnet.cellsToAttract.get(this);
      if(ca!=null){
        revealMagnet = true;
        cellMagnetColor = ca.attractionColor;
      }
    }
     
    leaveTrail = false;
    if(egg){
      if(isFriendSpawn)
        cellColorSet = color(eggOuterColor);
      else
        cellColorSet = color(150,150,150);
    }
    else
    if(isSelected)
      cellColorSet = color(245,90,138);
    else
    if(hoveringOver)
      cellColorSet = color(247,192,215); 
    else      
    if(revealMagnet)
      cellColorSet = cellMagnetColor;
    else    
    if(mouseOverMagnet && !revealMagnet)
      cellColorSet = color(60);
    else
    if(aCellSelected)
      cellColorSet = color(60,120);    
    else{
      cellColorSet = color(255,245);
      leaveTrail = true;
    }
      
    if(leaveTrail)      
      cellColor = lerpColor(cellColor, cellColorSet, .02);      
    else
      cellColor = lerpColor(cellColor, cellColorSet, .1);      
      
    if(hoveringOver)
      cellHistoryLineOpa = 160;           
    
    if(lastHoverOver != hoveringOver && !egg){
      if(hoveringOver==true){
        if(!sound.disableSound)        
          sound.historySound.play();      
      }
    }
    
    if(isSelected)
      friendLinkOpa = lerp(friendLinkOpa, 120, .2);
    else
    if(hoveringOver)
      friendLinkOpa = lerp(friendLinkOpa, 50, .2);
    else
      friendLinkOpa = lerp(friendLinkOpa, 0, .2);    
    
    lastHoverOver = hoveringOver;
        
    cellHistoryLineOpa = lerp(cellHistoryLineOpa, 0, .01);
    

    if(wandering){
      sfSingle(m, queryMass, queryRadius, .01);      
      sfSingle(m, queryMass, wanderDistance, .02);
      //float ang = getAng(m.p, queryMass.p);
      //Vec2 forceForward = newVec(orientation + ang, wanderDistance);
      //forceForward.mul(wanderSpeed);
      //m.v.add(forceForward);
    } 
    
    if(ignoreCollisions){
      m.v.nowEquals(0,0);
    }
    
    m.update();
    m.dampScalar(FRICTION);
    for(int i=0; i<subs.length ;i++){
      subs[i].update();
    }      
    }catch(Exception e){
      printLog("Reason: " + e);
      printLog("cell update failed!");
    }
  }
  void draw(){
    //Debug
//    for(int i=0; i<subs.length;i++){
//      if(subs[i]==subs[subs.length-1])
//        subs[i].draw();
//      subs[i].draw();
//    }        
                  
    strokeWeight(1);
    noFill();
    if(egg)
      stroke(eggColor);
    else    
      stroke(255,160);
    ellipse(m.p, radius / 2);
            
    noFill();
    if(!smallCell)
      strokeWeight(2);
    else
      strokeWeight(1);

    stroke(cellColor);
    
    //ellipse(historyPos,15);
    
/*
    if(isSelected)
      stroke(245,90,138);
    else
    if(hoveringOver)
      stroke(247,192,215); 
    else
    if(mouseOverMagnet)
      stroke(cellColor);
    else
      stroke(255,200);
*/      
    //fill(0);
    
    if(!smallCell){
      beginShape();  
      for(int i=0; i<subs.length-1; i++){
        curveVertex(subs[i].pos.x,subs[i].pos.y);         
      }
      curveVertex(subs[subs.length-1].pos.x,subs[subs.length-1].pos.y);     
      curveVertex(subs[0].pos.x,subs[0].pos.y);         
      curveVertex(subs[1].pos.x,subs[1].pos.y);      
      curveVertex(subs[2].pos.x,subs[2].pos.y);        
      endShape();  
    }
    else{      
      
      if(!egg)
        ellipse(m.p,radius);
      else
        ellipse(m.p, radius + 5 * sin(sinWave));
    }       
    
    if(user!=null){
      if(user.gearonUser){
        drawGearonIndicator();
      }
    }
    
    /*
    if(hoveringOver){
      pushMatrix();
      translate(m.p.x+radius*2,m.p.y+6);
      fill(cellColor);
      textAlign(LEFT);      
      textFont(guiFont,12);
      text(user.handle,0,0);
      popMatrix();   
    } */
    
    drawHistoryLine();
       
    drawFriendLinks();    
  }
  
  void useHistoryLine(){
    inHistoryLog = true;
    cellHistoryLineOpa = 180;
  }
  
  void drawHistoryLine(){
    if(inHistoryLog){
      
      if(cellHistoryLineOpa <=5){
        inHistoryLog = false;        
        return;
      }
      
      float sx = m.p.x + (historyPos.x - m.p.x) * .1;
      float sy = historyPos.y;
      historyAnchor.nowEquals(sx,sy);
      
      smooth();
      stroke(255,cellHistoryLineOpa);
      strokeWeight(1);
      noFill();
      
      curveTightness(.95);      
      beginShape();
      curveVertex(m.p);      
      curveVertex(m.p);
      curveVertex(historyAnchor);
      curveVertex(historyPos);
      curveVertex(historyPos);      
      endShape();
      curveTightness(-1);        
//      line(m.p, historyAnchor);
//      line(historyAnchor, historyPos);
      
      inHistoryLog = false;
    }
  }
  
  void  drawGearonIndicator(){
    if(!user.gearonUser)
      return;
    if(radius < 5)
      return;
    spinnerAngle+=2;
    pushMatrix();
    translate(m.p);
    rotate(radians(spinnerAngle));    
    pushMatrix();    
    float angle = radians((float)1/6.0*360.0);    
    for(int i=0;i<6;i++){
      rotate( angle );            
      strokeWeight(4);
      stroke(cellColor);
      line(radius*.5,0,radius*.85,0);      
    }
    popMatrix();    
    popMatrix();
  }
  
  Vec2 friendAnchor = new Vec2();
  float friendPathTimer = 0;
  Vector friendPath = new Vector();
  Vector friendPathTime = new Vector();
  float friendLinkOpa = 0;
  void drawFriendLinks(){
    if(friendLinkOpa < 1)
      return;
    
    if(user.flickrFriends.size() <= 0)
      return;                     
      
    strokeWeight(1);
    for(int i=0; i<user.flickrFriends.size(); i++){
      User friend = (User) user.flickrFriends.get(i);
      Cell friendCell = friend.cell;      
      
      float sx = m.p.x + (friendCell.m.p.x - m.p.x) * .1;
      float sy = friendCell.m.p.y;
      friendAnchor.nowEquals(sx,sy);         
      
      gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_DST_ALPHA);                                  
      smooth();
      noFill();
      pushMatrix();
            
      stroke(#7EF25F,friendLinkOpa / 2);                 
      beginShape(); 
      curveVertex(m.p);
      curveVertex(m.p);      
      
      curveVertex(friendAnchor);
      
      stroke(#C0F08A,friendLinkOpa);
      curveVertex(friendCell.m.p);
      curveVertex(friendCell.m.p);      
      endShape();
      
      
      if(hoveringOver || isSelected){
        ellipse(friendCell.m.p,15);
      
        Float fl = (Float) friendPathTime.get(i);
        friendPathTimer = fl.floatValue();
        friendPathTimer = lerp(friendPathTimer, 1, .05);
        if(friendPathTimer >= .998)
          friendPathTimer = 0.01;      
        
        fl = new Float(friendPathTimer);
        friendPathTime.set(i,fl);
      
        Vec2 follower = (Vec2) friendPath.get(i);
        follower.nowEquals(curvePoint(m.p.x, friendAnchor.x, friendCell.m.p.x, friendCell.m.p.x, friendPathTimer), 
                           curvePoint(m.p.y, friendAnchor.y, friendCell.m.p.y, friendCell.m.p.y, friendPathTimer));
        //stroke(255, ((friendPathTimer * 205) + 50) * (friendLinkOpa / 255)  );

//        gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_DST_ALPHA);                                
//        gl.glBlendFunc(gl.GL_ONE, gl.GL_ONE);
        tint(180,253,56,((friendPathTimer * 205) + 180) * (friendLinkOpa / 255)  );
        pushMatrix();
        translate(follower);
        translate(-16, -16);        
//        scale(1.4);
//        scale( ( .05 / friendPathTimer) );        
        image(tinyGlow,0,0);
        popMatrix();
        noTint();
//        ellipse(follower, (2 + .05/ friendPathTimer) * 2);
      }
      gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);            
      popMatrix();
    }
  }
}

class SubCell{
  Vec2 pos;
  float radius = 15;
  SubCell linked[];
  mass m;
  SubCell(Vec2 pos){
    m = new mass(pos);
    this.pos = new Vec2(pos);    
  }
    
  void update(){
//    m.v.add(new Vec2(0,.1));
    m.update();
    m.dampScalar(FRICTION);
    pos.x = m.p.x;
    pos.y = m.p.y;
  }
  
  void draw(){
    noFill();
    stroke(0,255,20);
    ellipse(pos,radius);
  }
  
  void connectWith(SubCell c){
    m.connectSpringTo(c.m, springConstant);
  }
}

void drawCellHistoryLines(){
  for(int i=0; i<cells.size(); i++){
    Cell cell = (Cell) cells.get(i);
    cell.drawHistoryLine();
  }
}

void updateCells(){
  //this will give our larger ellipses a hexagonal look
  curveTightness(-1);  
  hoveringCell = null;
  mouseOverCell = false;
  for(int i=0;i<cells.size();i++){
    Cell cell = (Cell) cells.get(i);
    cell.update();
    cell.draw();    
    
    if(cell.hoveringOver){
      mouseOverCell = true;
      hoveringCell = cell;
    }
  }
  if(selectedCell == null)
    aCellSelected = false;
  else
    aCellSelected = true;


  //the below is deprecated
  //now using RDC for collision handling
  
  recursiveClustering(cells, 0, 1);  
  
//  if(mousePressed){
//    for(int i=0;i<cells.length;i++){
//      sfSingle(cells[i].m,new mass(mouseX,mouseY), 0,  cells[i].weight);//.001 + ( ( cells[i].weight) * .02));    
//    }
//  }

//  float testDistance = 10;
//  float d1=0;
//  float d2=0;
//  float d3=0;
//  float minDistance;
//  Cell cell_i;
//  Cell cell_s;
  
  
  //cell to cell collisions
  //this stands to be heavily optimized  
  /*
  for(int i=0; i<cells.size();i++){        
    cell_i = (Cell) cells.get(i);    
    for(int s=i+1; s<cells.size(); s++){
      cell_s = (Cell) cells.get(s);      
      d2 = dist(cell_i.m.p, cell_s.m.p);
      minDistance = cell_i.radius + cell_s.radius;
            if(d2 < minDistance*.8)
             sfDouble(cell_i.m, cell_s.m, minDistance*.8, .6);

      for(int a=0; a<cell_i.subs.length; a++){
        d3 = dist(cell_i.subs[a].m.p, cell_s.m.p);
        if(d3 < minDistance*.8)
          sfDouble(cell_i.subs[a].m, cell_s.m, minDistance*.8, .8);

        for(int b=0; b<cell_s.subs.length; b++){
          d1 = dist(cell_i.subs[a].m.p, cell_s.subs[b].m.p);
          if(d1 < testDistance){
            sfDouble(cell_i.subs[a].m, cell_s.subs[b].m, testDistance, .8);
          }
        }
      } 
    }
  }  
  
 */
}

void selectCell(Cell cell){
  if(cell == null)
    return;
  
  //eggs can't be selected
  //saves a lot of headaches...
  //you shouldn't be touching the younglings, anyway  
  //pervert
  if(cell.egg)
    return;
    
  //play a sound
  if(!sound.disableSound)
    sound.itemClickSound.play();  
    
    
  Cell lastSelectedCell = selectedCell;  
  
  if(lastSelectedCell == cell)
    return;
  
  if(selectedCell != null)
    if(selectedCell.user!=null)   
        selectedCell.user.clearImages();
        
  selectedCell = cell;
  setUserToDraw(selectedCell);
  addToUserLog(selectedCell);    
        
  //onclick, begin streaming images
  if(selectedCell.user!=null)
      selectedCell.user.streamImages(); 
        
  //reset userinfo display animations
    resetUserDisplay();  
              
}
