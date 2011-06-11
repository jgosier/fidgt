//2D Physics Library
//by Michael Chang, UCLA
//http://users.design.ucla.edu/~mflux
//May 21, 2005

//  I won't care to explain my physics here, but please feel free to poke around.
//  It's not difficult.

Vector masses = new Vector();

boolean WORLDBORDER=false;
float WORLDBORDERBUFFER=-30;
float FRICTION=0.90;
Vec2 GRAVITY=new Vec2(0,.4);

boolean DEBUGSPRINGS = false;
class mass
{
  Vec2 p;        //spatial position
  Vec2 v;        //velocity Vec2tor
  Vec2 a;        //acceleration Vec2tor
  mass cm[];
  float sl[];
  float sk[];
  float x()
  {
    return p.x;
  }
  float y()
  {
    return p.y;
  }
  mass()
  {
    p=new Vec2();
    v=new Vec2();
    a=new Vec2();
    masses.add(this);
  }
  mass(mass m)
  {
    this.p=new Vec2(m.p);
    this.v=new Vec2(m.v);
    this.a=new Vec2(m.a);
    if(cm==null)
      return;
    cm=new mass[m.cm.length];
    for(int i=0;i<cm.length;i++)
    {
      cm[i]=m.cm[i];
    }
//    System.arraycopy(m.cm,0,cm,0,cm.length);
    sl=new float[m.sl.length];
    System.arraycopy(m.sl,0,sl,0,sl.length);    
    sk=new float[m.sk.length];
    System.arraycopy(m.sk,0,sk,0,sk.length);     
    masses.add(this);    
  }
  mass(float x,float y)
  {
    p=new Vec2(x,y);
    v=new Vec2();
    a=new Vec2();
    masses.add(this);    
  }
  mass(Vec2 p)
  {
    this.p=new Vec2(p);
    v=new Vec2();
    a=new Vec2();
    masses.add(this);    
  }
  mass(Vec2 p,Vec2 v)
  {
    this.p=new Vec2(p);
    this.v=new Vec2(v);
    a=new Vec2();
    masses.add(this);    
  }
  mass(Vec2 p,Vec2 v,Vec2 a)
  {
    this.p=new Vec2(p);
    this.v=new Vec2(v);
    this.a=new Vec2(a);
    masses.add(this);    
  }
  void update()
  {
//    v.add(a);
    p.add(v);
    if(WORLDBORDER)
      borders(WORLDBORDERBUFFER);
      
    if(cm!=null)
      updateSprings();
  }
  void dampScalar(float k)
  {
    v.mul(k);
  }
  void borders(float buff)
  {
    if(p.x>width+buff)
    {
      p.x=width+buff;      
//      Vec2 nextV=new Vec2(p);
//      nextV.add(v);
//      sfSingle(this,new mass(nextV),0,-.5);
    }
    if(p.x<-buff)
    {
      p.x=-buff; 
//      Vec2 nextV=new Vec2(p);
//      nextV.add(v);
//      sfSingle(this,new mass(nextV),0,-.5);
    }
    if(p.y>height+buff)
    {
      p.y=height+buff;
//      Vec2 nextV=new Vec2(p);
//      nextV.add(v);
//      sfSingle(this,new mass(nextV),0,-.5);
    }
    if(p.y<-buff)
    {
      p.y=-buff;      
//      Vec2 nextV=new Vec2(p);
//      nextV.add(v);
//      sfSingle(this,new mass(nextV),0,-.5);
    }
  }

  void connectSpringTo(mass m,float springLength,float springConstant)
  {
    if(m==null)
      return;
    if(cm==null)
      cm=new mass[0];
    mass temp[]=new mass[cm.length+1];
    System.arraycopy(cm,0,temp,0,cm.length);
    temp[cm.length]=m;
    cm=temp;
    if(sl==null)
      this.sl=new float[0];
    this.sl=append(sl,springLength);
    if(sk==null)
      this.sk=new float[0];
    this.sk=append(sk,springConstant);
  }
  void connectSpringTo(mass m,float k)
  {  
    float d=dist(this.p,m.p);
    connectSpringTo(m,d,k);
  }  
  void updateSprings()
  {
    if(cm==null)
      return;      
    for(int i=0;i<cm.length;i++)    
    {
       try{
         sfDouble(this,cm[i],sl[i],sk[i]);
       }
       catch(Exception e){
         printLog("total springs " + cm.length);
         printLog("reason: " + e);
         printLog("spring failed");         
       }
       
       if(DEBUGSPRINGS){
         strokeWeight(1);
         noFill();
         stroke(0,90,255,80);
         line(this.p, cm[i].p);
       }
    }
  }
  void clearSprings()
  {
    cm=null;
    sk=null;
    sl=null;
  }
}
Vec2 centerOfMass(mass m[])
{
  if(m==null||m.length==0)
    return null;
  Vec2 v=new Vec2();
  for(int i=0;i<m.length;i++)
  {
    v.add(m[i].p);
  }
  v.div(float(m.length));
  return v;
}
mass[] rotateMasses(mass m[],Vec2 v,float ang)
{
  mass nm[]=new mass[m.length];  
  for(int i=0;i<m.length;i++)
  {
    nm[i]=new mass(m[i]);
    nm[i].p.rotate(v,ang);
  }
  return nm;
}

//November:
//Optimizations to this piece of code gave 3x performance!!
//Basically... not calling new() every call to springForce but rather
//just reusing the same memory-space.
Vec2 displace = new Vec2();
Vec2 sf = new Vec2();
Vec2 springForce(mass a,mass b,float rl,float k)
{
  displace.nowEquals(a.p,b.p);
  displace.mul(.5);
  float dl=displace.mag();
  if(dl==0)
    return new Vec2();
  float ratio=rl/2/dl;
  sf.nowEquals(displace.x - displace.x*ratio , displace.y - displace.y*ratio);
  sf.mul(k);
  return sf;
}

Vec2 springForcePreDistance(mass a,mass b, float dl, float rl,float k)
{
  displace.nowEquals(a.p,b.p);
  displace.mul(.5);
  dl /= 2;
  if(dl==0)
    return new Vec2();
  float ratio=rl/2/dl;
  sf.nowEquals(displace.x - displace.x*ratio , displace.y - displace.y*ratio);
  sf.mul(k);
  return sf;
}

void sfSingle(mass a,mass b, float rl,float k)
{
//  Vec2 sf=springForce(a,b,rl,k);
  a.v.add(springForce(a,b,rl,k));
}

void sfSinglePD(mass a,mass b, float dl,float rl,float k)
{
//  Vec2 sf=springForce(a,b,rl,k);
  a.v.add(springForcePreDistance(a,b,dl,rl,k));
}

void sfDouble(mass a,mass b, float rl,float k)
{
//  Vec2 sf=springForce(a,b,rl,k);
  sf = springForce(a,b,rl,k);
  a.v.add(sf);
  b.v.sub(sf);  
}

void sfDoublePD(mass a,mass b, float dl, float rl,float k)
{
//  Vec2 sf=springForce(a,b,rl,k);
  sf = springForcePreDistance(a,b, dl, rl,k);
  a.v.add(sf);
  b.v.sub(sf);  
}
