

(
// using built-in SC stuff
var spec, synth, win, gui;
spec = ControlSpec(20,20000,\exp,1,440,"Hz");
synth = Synth("testsine", [\freq, 440]);
win = Window.new("gui", Rect(200, 200, 100, 200));
gui = Slider.new(win, Rect(10, 10, 20, 150));
gui.action_({|slider| 
synth.set(\freq, spec.map(slider.value))
});
win.front;
)

(

c = CCResponder({ |src,chan,num,value|
[src,chan,num,value].postln;
},
nil, // any source
nil, // any channel
nil, // any CC number
nil // any value
)
)

c.remove

(
c = CCResponder({ |src,chan,num,value|
[src,chan,num,value].postln;
},
nil, // any source
nil, // any channel
80, // CC number 80
{ |val| val < 50 } // any value less than 50
)
)
CCResponder.removeAll

(
p = Pbind(
	\instrument, \bubblebub,
	\sizefactor, Pwhite(0,1,inf),
	\dur, Pgauss(0.3, 0.2),
	\freq,  Pkey(\sizefactor).linexp(0, 1, 1000, 3000),
	\amp ,  Pkey(\sizefactor).linlin(0, 1, 0.15, 0.04), 
	\decay, Pkey(\sizefactor).linlin(0, 1, 0.05, 0.08), 
	\doneAction, 2
).play
)

(
p = Pbind(
	\instrument, \bubblebub,
	// The commented version is a bit like the above timings...
	// \dur, Pseq([29, 37, 47, 67, 89, 113, 157, 197, 200].differentiate * 0.015, inf),
	// ...but happily we have useful random-distrib generators. Ppoisson would be ideal but is misbehaving for me!
	\dur, Pgauss(0.3, 0.2),
	\freq, Pwhite(0,1,inf).linexp(0,1, 1000, 3000),
	// doneAction of two allows the synths to free themselves. See  "UGen-doneActions".openHelpFile
	\doneAction, 2
).play
)
x = Synth(\bubblebub);
x.set(\t_trig, 1); // run this line multiple times, to get multiple (very similar) bubbles!
x.free;

