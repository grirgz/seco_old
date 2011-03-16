s.boot
(
var makePattern;

makePattern = { arg box;
       box.list = box.list.copy.stutter(1);
       Pbind(
               'note',Pseq(box.list,2),
               \dur,Pfunc({(box.point.x+1)/8}),
               \root,Pfunc({x.numRows-box.point.y}));
};

x = BoxMatrix(nil,nil,15,32);

b = x.at(3@3);
b.title = "tinkle";
b.boxColor = Color.rand;
b.list = Array.rand(8,0,24);
b.pattern = makePattern.value(b);
b.pattern.play;

x.onBoxDrag = { arg fromBox,toBox,modifier;
       var newTitle;

       if(modifier.isAlt,{
               // option drag copies
               x.copy(fromBox.point,toBox.point);
               toBox.pattern = makePattern.value(x.at(toBox.point));
               toBox.pattern.play;
       },{
               // else move it
               x.move(fromBox.point,toBox.point);
               //x.at(toBox.point).pattern.play
       });

};


)
s.scVersionMajor
