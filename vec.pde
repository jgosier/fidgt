//2D Vector Library
//Copyright (C) Michael "Flux" Chang (www.ghost-hack.com) 2006

Vec2 origin2=new Vec2();    //origin is 0,0
//--------------BEGINNING OF VECTOR CLASS OBJECT--------------
class Vec2
{
  float x;
  float y;
  Vec2()
  {//constructor with no arguments, gives you origin
    x=0;
    y=0;
  }
  Vec2(float x,float y)
  {//constructor with 2 arguments x and y
    this.x=x;
    this.y=y;
  }
  Vec2(Vec2 s)
  {//constructor with Vec as input. basically an assignment operator
    if(s==null)
      return;
    this.x=s.x;
    this.y=s.y;
  }  
  Vec2(Vec2 s,Vec2 t)
  {//constructs a Vector from the difference of two Vectors
    float dx=t.x-s.x;
    float dy=t.y-s.y;
    this.x=dx;
    this.y=dy;
  }
  Vec2(Vec2 s,Vec2 t,float k)
  {//constructs a Vector from the difference of two Vectors, then modifies it by k
    float dx=t.x-s.x;
    float dy=t.y-s.y;
    this.x=dx*k;
    this.y=dy*k;
  }

  //--------------BEGINNING OF VECTOR METHODS--------------  
  void disp(float ang,float magnitude)
  {//displacement. will offset this Vector by an angle and distance
    ang=radians(ang);
    x+=cos(ang)*magnitude;
    y-=sin(ang)*magnitude;    
  }
  void rotate(float ang)
  {
    Vec2 temp=new Vec2();
    temp.disp(ang()+ang,mag());
    x=temp.x;
    y=temp.y;
  }  
  void rotate(Vec2 v,float ang)
  {
    Vec2 cen=new Vec2(v);
    Vec2 ori=new Vec2(v,this);
    ori.rotate(180+ang);
    cen.add(ori);
    this.x=cen.x;
    this.y=cen.y;
    return;
  }
  float ang()
  {//returns the angle between this Vector and origin
    return getAng(this,origin2);
  }
  float mag()
  {//returns the distance between this Vector and origin
    return dist(origin2,this);
  }
  //scalar operations
  void add(float s)
  {//addition operator
    x+=s;
    y+=s;
  }
  void sub(float s)
  {//subtraction operator
    x-=s;
    y-=s;
  }
  void mul(float s)
  {//multiplication operator
    x*=s;
    y*=s;
  }
  void div(float s)
  {//division operator. returns 0 when division by zero
    if(s==0)
      return;
    x/=s;
    y/=s;
  }
  void add(float x,float y)
  {//addition operator
    this.x+=x;
    this.y+=y;
  }
  void sub(float x,float y)
  {//subtraction operator
    this.x-=x;
    this.y-=y;
  }
  void mul(float x,float y)
  {//multiplication operator
    this.x*=x;
    this.y*=y;
  }
  void div(float x,float y)
  {//division operator. returns 0 when division by zero
    if(x==0||y==0)
      return;
    this.x/=x;
    this.y/=y;
  }  
  //Vector operators
  void add(Vec2 s)
  {//addition operator
    x+=s.x;
    y+=s.y;
  }
  void sub(Vec2 s)
  {//subtraction operator
    x-=s.x;
    y-=s.y;
  }
  void mul(Vec2 s)
  {//multiplication operator
    x*=s.x;
    y*=s.y;
  }
  void div(Vec2 s)
  {//division operator. returns 0 when division by zero
    if(s.x==0||s.y==0)
      return;
    x/=s.x;
    y/=s.y;
  }  
  
  void nowEquals(float x, float y){
    this.x = x;
    this.y = y;
  }  
  void nowEquals(Vec2 s){
    this.x = s.x;
    this.y = s.y;
  }
  void nowEquals(Vec2 s,Vec2 t)
  {//constructs a Vector from the difference of two Vectors

    this.x=t.x-s.x;
    this.y=t.y-s.y;
  }
  
}
//--------------END OF VECTOR OBJECT--------------
//--------------ALTERNATE METHODS OF CONSTRUCTING A VECTOR--------------
Vec2 newVec(Vec2 v,float ang,float magnitude)
{
  Vec2 temp=new Vec2(v); 
  temp.disp(ang,magnitude);
  return temp;
}

Vec2 newVec(float ang,float magnitude)
{
  ang=radians(ang);
  float tx=cos(ang)*magnitude;
  float ty=sin(ang)*magnitude*-1;
  Vec2 temp=new Vec2(tx,ty);  
  return temp;
}
Vec2 nv(float x,float y)
{
  return new Vec2(x,y);
}
Vec2 nv(Vec2 a,Vec2 b)
{
  return new Vec2(a,b);
}
Vec2 nv(Vec2 v)
{
  return new Vec2(v.x,v.y);
}

//--------------USEFUL VECTOR TOOLS--------------
//Finds the midpoint between two positions in space
Vec2 midPoint(Vec2 a,Vec2 b)
{
  Vec2 d=new Vec2(a,b);
  d.mul(.5);
  Vec2 dest=new Vec2(a);
  dest.add(d);
  //  d.sub(b);
  //  Vec newVec=new Vec(a);
  //  newVec.add(d);
  return dest;
}

//Finds the average position in an array of points in space
Vec2 avg(Vec2 v[])
{
  Vec2 total=new Vec2();
  for(int i=0;i<v.length;i++)
  {
    total.add(v[i]);
  }
  total.div(v.length);
  return total;
}

//Using point a as origin, this returns the angle between a and b.
float getAng(Vec2 a,Vec2 b)
{
  float ang=atan2(-1*(b.y-a.y),b.x-a.x);
  ang=degrees(ang);
  if(b.y>a.y)
    return ang=360+ang;
  if(b.y==a.y&&b.x>a.x)
    return ang=0;    
  if(b.y==a.y&&b.x<a.x)
    return ang=180;
  return ang;
}

//Returns the distances between two points in space
float dist(Vec2 a,Vec2 b)
{
  if(a==null||b==null)
    return 0;
  return dist(a.x,a.y,b.x,b.y);
}

//Returns an interpolated point between two points in space, given a ratio
Vec2 lerpN = new Vec2();
Vec2 lerp(Vec2 a,Vec2 b,float k)
{
  lerpN = new Vec2();
  lerpN.x=lerp(a.x,b.x,k);
  lerpN.y=lerp(a.y,b.y,k);
  return lerpN;
}

void lerp(Vec2 v, Vec2 source, Vec2 target, float k){
  v.x = lerp(source.x, target.x, k);
  v.y = lerp(source.y, target.y, k);
}

//Wraps a point in space from 0 to canvas edge, given a buffer
Vec2 wrap(Vec2 v,float buff)
{
  if(v.x<0-buff)
    v=nv(width/2,height/2);
  if(v.x>width+buff)
    v=nv(width/2,height/2);
  if(v.y<0-buff)
    v=nv(width/2,height/2);
  if(v.y>height+buff)
    v=nv(width/2,height/2);
  return v;
}

//Constrains a point in space from 0 to canvas edge, given a buffer
Vec2 constrain(Vec2 v,float buff)
{
  Vec2 temp=new Vec2(v);
  temp.x=constrain(temp.x,-buff,width+buff);
  temp.y=constrain(temp.y,-buff,height+buff);
  return temp;
}

//Appends a Vector point at the end of a list of Vectors
Vec2[] append(Vec2 v[],Vec2 nv)
{
  Vec2 temp[]=new Vec2[v.length+1];
  System.arraycopy(v,0,temp,0,v.length);
  temp[v.length]=new Vec2(nv);
  return temp;  
}

//A Segment is an object consisting of two points. This makes a line...
class Seg
{
  Vec2 a;
  Vec2 b;
  Seg()
  {
    a=new Vec2();
    b=new Vec2();
  }
  Seg(Vec2 a,Vec2 b)
  {
    this.a=new Vec2(a);
    this.b=new Vec2(b);
  }
  float mag()
  {
    return dist(a,b);
  }
}

//Sedgwick's Line Intersection algorithm
//Will return 1 if p1 to p2 to p3 is found to be rotating counter clockwise
int CCW(Vec2 p1, Vec2 p2, Vec2 p3)
{
  float dx1, dx2, dy1, dy2;
  dx1 = p2.x - p1.x; 
  dy1 = p2.y - p1.y;
  dx2 = p3.x - p1.x; 
  dy2 = p3.y - p1.y;
  if (dx1*dy2 > dy1*dx2) return +1;
  if (dx1*dy2 < dy1*dx2) return -1;
  if ((dx1*dx2 < 0) || (dy1*dy2 < 0)) return -1;
  if ((dx1*dx1+dy1*dy1) < (dx2*dx2+dy2*dy2)) 
    return +1;
  return 0;
}

//Given lines A1A2, B1B2, this returns true if they are intersecting
boolean intersect(Vec2 a1, Vec2 a2, Vec2 b1, Vec2 b2)
{  
  return
    ((CCW(a1, a2, b1) != CCW(a1, a2, b2))
    && (CCW(b1, b2, a1) != CCW(b1, b2, a2)));
}

//Given line Segment A and B, returns true if found intersecting
boolean intersect(Seg a,Seg b)
{  
  if(a==null||b==null)
    return false;
  Vec2 a1=a.a;
  Vec2 a2=a.b;
  Vec2 b1=b.a;
  Vec2 b2=b.b;
  return intersect(a1,a2,b1,b2);
}

//Given line Segment A and B, returns the point of intersection
Vec2 intersectPoint(Seg a,Seg b)
{
  Vec2 n=new Vec2();
  float x1=a.a.x;
  float x2=a.b.x;
  float x3=b.a.x;
  float x4=b.b.x;
  float y1=a.a.y;
  float y2=a.b.y;
  float y3=b.a.y;
  float y4=b.b.y;
  float nd=((y4-y3)*(x2-x1))-((x4-x3)*(y2-y1));
  if(nd==0)
    return a.a;
  float mnx=(((x4-x3)*(y1-y3))-((y4-y3)*(x1-x3)))   /   nd;
  //  float mny=(((x2-x1)*(y1-y3))-((y2-y1)*(x1-x3)))   /   nd;
  float nx=x1+mnx*(x2-x1);
  float ny=y1+mnx*(y2-y1);
  n=new Vec2(nx,ny);
  return n;
}

//Given line Segment A and B, returns the point of intersection
//Alternate method
Vec2 intersectPoint2(Seg a,Seg b)
{
  Vec2 n=new Vec2();
  float A1=a.a.x-a.b.x;
  float B1=a.a.y-a.b.y;
  float C1=A1*a.a.x+B1*a.a.y;
  float A2=b.a.x-b.b.x;
  float B2=b.a.y-b.b.y;
  float C2=A2*b.a.x+B2*b.a.y;
  float det=(A1*B2)-(A2*B1);
  if(det==0)
  {
    return new Vec2();
  }
  else
  {
    n.x=((B2*C1 - B1*C2)/det);
    n.y=((A1*C2 - A2*C1)/det);
  }
  return n;
}

//Normalizes an angle between 0 and 360
float normalize(float a)
{
  if(a>360)
    return a%360;
  if(a<0)
    return 360+(a%360);
  return a;
}
//Wraps a number inside a range, from minimum to maximum
float wrap(float n,float mini,float maxi)
{
  n=n%maxi;
  if(n<mini)
    return maxi-(mini-n);
  if(n>maxi)
    return mini+(n-maxi);
  return n;
}
void ellipse(Vec2 p,float r)
{
  ellipse(p.x,p.y,r,r);
}
void line(Vec2 a,Vec2 b)
{
  line(a.x,a.y,b.x,b.y);
}
void println(Vec2 v)
{
  println("point: "+v.x+","+v.y);
}
void curveVertex(Vec2 v){
  curveVertex(v.x,v.y);
}
void curveVec3(Vec2 v)
{
  curveVertex(v.x,v.y);
}
void bezier(Vec2 a,Vec2 b,Vec2 c,Vec2 d)
{
  bezier(a.x,a.y,b.x,b.y,c.x,c.y,d.x,d.y);
}
Vec2 bezierPoint(Vec2 a,Vec2 b,Vec2 c,Vec2 d,float t)
{
  float nx=bezierPoint(a.x,b.x,c.x,d.x,t);
  float ny=bezierPoint(a.y,b.y,c.y,d.y,t);  
  return new Vec2(nx,ny);
}
void translate(Vec2 v)
{
  translate(v.x,v.y);
}
boolean inRect(Vec2 v, float left, float right, float top, float bottom){
  if(v.x>left && v.x<right && v.y>top && v.y<bottom)
    return true;
  else return false;
}

void vertex(Vec2 v)
{
  vertex(v.x,v.y);
}
