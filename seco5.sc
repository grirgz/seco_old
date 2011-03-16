
s.boot
(
SynthDef("channel", {
	arg out, in, amp, bal;
	Out.ar(out, In.ar(in)*amp);

}).send(s);

SynthDef("sin1", {
	arg out, t_trig;
	Out.ar(out, In.ar(in)*amp);

}).send(s);
)
(
SynthDef(\bubblebub, {	|out=0, t_trig=0, attack=0.01, decay=0.08, pitchcurvelen=0.1, freq=1000, doneAction=0, amp=0.1|
	var pitch, son;
	amp   = amp * EnvGen.ar(Env.perc(attack, decay).delay(0.003), t_trig, doneAction: doneAction);
	pitch = freq * EnvGen.ar(Env.new([0,0,1],[0,1]).exprange(1, 2.718), t_trig, timeScale: pitchcurvelen);
	son = SinOsc.ar(pitch);
	// high-pass to remove any lowpitched artifacts, scale amplitude
	son = HPF.ar(son, 500) * amp * 10;
	Out.ar(out, son);
}).store
)
 
x = Synth(\bubblebub);

x.set(\t_trig, 1); // run this line multiple times, to get multiple (very similar) bubbles!
x.free;

(

// define a noise pulse

SynthDef("tish", { arg freq = 1200, rate = 2;

var osc, trg;

trg = Decay2.ar(Impulse.ar(rate,0,0.3), 0.01, 0.3);

osc = {WhiteNoise.ar(trg)}.dup;

Out.ar(0, osc); // send output to audio bus zero.

}).send(s);

)


(

// define an echo effect

SynthDef("echo", { arg delay = 0.2, decay = 4;

var in;

in = In.ar(0,2);

// use ReplaceOut to overwrite the previous contents of the bus.

ReplaceOut.ar(0, CombN.ar(in, 0.5, delay, decay, 1, in));

}).send(s);

)


// start the pulse

s.sendMsg("/s_new", "tish", x = s.nextNodeID, 1, 1, \freq, 200, \rate, 1.2);


// add an effect

s.sendMsg("/s_new", "echo", y = s.nextNodeID, 1, 1);


// stop the effect

s.sendMsg("/n_free", y);


// add an effect (time has come today.. hey!)

s.sendMsg("/s_new", "echo", z = s.nextNodeID, 1, 1, \delay, 0.1, \decay, 4);


// stop the effect

s.sendMsg("/n_free", z);


// stop the pulse

s.sendMsg("/n_free", x);
