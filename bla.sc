(

w = SCWindow.new;


c = SCCompositeView(w,Rect(0,0,300,300));


a = SC2DSlider(c,Rect(0,0,100,100));

b = SC2DSlider(c,Rect(100,100,100,100));


c.background = Gradient(Color.rand,Color.rand);


w.front;

)
