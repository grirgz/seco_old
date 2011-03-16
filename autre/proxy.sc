///////////////////// using node proxy with ugen functions  ///////////////////// 


s.boot;


a = NodeProxy.audio(s, 2);

a.play; // play to hardware output, return a group with synths


// setting the source

a.source = { SinOsc.ar([350, 351.3], 0, 0.2) };


// the proxy has two channels now:

a.numChannels.postln;

a.source = { SinOsc.ar([390, 286] * 1.2, 0, 0.2) };


// exeeding channels wrap:

a.source = { SinOsc.ar([390, 286, 400, 420, 300] * 1.2, 0, 0.2) };


// other inputs

a.source = { WhiteNoise.ar([0.01,0.01]) };

a.source = 0;

a.source = \default; // synthDef on server

a.source = SynthDef("w", { arg out=0; Out.ar(out,SinOsc.ar([Rand(430, 600), 600], 0, 0.2)) });

a.source = nil; //  removes any object


// feedback

a.source = { SinOsc.ar(a.ar * 7000 * LFNoise1.kr(1, 0.3, 0.6) + 200, 0, 0.1) };

a.source = { SinOsc.ar(a.ar * 6000 * MouseX.kr(0, 2) + [100, 104], 0, 0.1) };


// fadeTime

a.fadeTime = 2.0;

a.source = { SinOsc.ar([390, 286] * ExpRand(1, 3), 0, 0.2) };



// adding nodes

a.add({ SinOsc.ar([50, 390]*1.25, 0, 0.1) });

a.add({ BrownNoise.ar([0.02,0.02]) });


// setting nodes at indices:

a[0] = { SinOsc.ar( 700 * LFNoise1.kr(1, 0.3, 0.6) + 200, 0, 0.1) };

a[1] = { LFPulse.kr(3, 0.3) * SinOsc.ar(500, 0, 0.1) };

a[2] = { LFPulse.kr(3.5, 0.3) * SinOsc.ar(600, 0, 0.1) };

a[3] = { SinOsc.ar([1,1.25] * 840, 0, 0.1) };


// filtering: the first argument is the previous bus content. more args can be used as usual.

a[3] = \filter -> { arg in; in * SinOsc.ar(Rand(100,1000)) };

a[2] = \filter -> { arg in; in * MouseY.kr(0,1) };

a[8] = \filter -> { arg in; in * MouseX.kr(0,1) };

a[4] = \filter -> { arg in; in * SinOsc.ar(ExpRand(1,5)).max(0) };

a.sources


// setting controls

a.fadeTime = 2.0;

a.source = { arg f=400; SinOsc.ar(f * [1,1.2] * rrand(0.9, 1.1), 0, 0.1) };

a.set(\f, rrand(900, 300));

a.set(\f, rrand(1500, 700));

a.xset(\f, rrand(1500, 700)); // crossfaded setting

a.source = { arg f=400; RLPF.ar(Pulse.ar(f * [1,1.02] * 0.05, 0.5, 0.2), f * 0.58, 0.2) };


// control lags

a.lag(\f, 1.5); // the objects are built again internally and sent to the server.

a.set(\f, rrand(1500, 700));

a.lag(\f, nil);

a.set(\f, rrand(1500, 700));


a.fadeTime = 1.0;


// mapping controls to other node proxies


c = NodeProxy.control(s, 2);

c.source = { SinOsc.kr([10,20] * 0.1, 0, 150, 1300) };

a.map(\f, c);

a[0] = { arg f=400; RHPF.ar(Pulse.ar(f * [1,1.2] * 0.05, 0.5, 0.2), f * 0.58, 0.2) };

c.source = { SinOsc.kr([10,16] * 0.02, 0, 50, 700) };

c.source = { Line.kr(300, 1500, 10) + SinOsc.kr(20 * [1,2], 0, 100) };

a[1] = { arg f; LFPar.ar(f % MouseX.kr(1, 40, 1) * 4 + 360, 0, 0.2) };

a.play

// map multiple channels of one proxy to multiple controls of another

// recently changed behaviour!


a.source = { arg f=#[400, 400]; LPF.ar(Pulse.ar(f[0] * [0.4,1], 0.2, 0.2), f[1] * 3) };

a.map(\f, c); // multichannel proxy c is mapped to multichannel control of a

a.source = { arg f=#[400, 400]; LPF.ar(Pulse.ar(f, 0.2, 0.2), f[1]) };

a.source = { arg f=#[400, 400]; Formant.ar(140, f * 1.5, 100, 0.1)  };

c.source = { SinOsc.kr([Line.kr(1, 30, 10), 1], 0, [100, 700], [300, 700]) };

c.source = 400;

a.play


c.fadeTime = 5.5;

c.source = { LFNoise0.kr([2.3, 1.0], [100, 700], [300, 1700]) };

c.source = { SinOsc.kr([2.3, 1.0], 0, [100, 700], [300, 1700]) };

c.source = 400;



// behave like a sc2 plug

c.gate(1400, 0.1);

c.gate(1000, 0.1);

c.line(1000, 1);


// direct access

a.lineAt(\f, 300, 2);

a.xlineAt(\f, 600, 0.3);

a.gateAt(\f, 1600, 0.3);



// changing nodeMaps

a.unmap(\f);

n = a.nodeMap.copy;

n.set(\f, 700);

a.fadeToMap(n);

n = a.nodeMap.copy;

n.set(\f, 400);

a.fadeTime = 1.0;

a.fadeToMap(n, [\f]); // linear interpolation to new map: experimental

a.map(\f, c); // restore mapping



// sending envelopes (up to 8 levels)

w = Env.new(Array.rand(3, 400, 1000),Array.rand(2, 0.3, 0.001), -4);

c.env(w);

c.env(w);

w = Env.new(Array.rand(8, 400, 1000),Array.rand(7, 0.03, 0.1));

c.env(w);

c.env(w);


// stop synthesis, then wake up proxies:


a.stop; // stop the monitor

a.play; // start the monitor

a.end; // release the synths and stop the monitor

c.free;  // free the control proxy c






///////////////////// channel offset/object index  ///////////////////// 



a = NodeProxy.audio(s,2);

a.play;

a[0] = { Ringz.ar(Impulse.ar(5, 0, 0.1), 1260) };

a.put(1, { Ringz.ar(Impulse.ar(5.3, 0, 0.1), 420) }, 1);

a.put(0, { Ringz.ar(Dust.ar([1,1]*15.3,  0.1), 720) }, 1);

a.put(1, { Ringz.ar(Impulse.ar(5.3, 0, 0.1), 420) }, 1);

a.end;





///////////////////// beat accurate playing  ///////////////////// 





a = NodeProxy.audio(s,2);

a.play;


a.clock = TempoClock(2.0).permanent_(true); // round to every 2.0 seconds

a.source = { Ringz.ar(Impulse.ar(0.5, 0, 0.3), 3000, 0.01) };

a[1] = { Ringz.ar(Impulse.ar([0.5, 1], 0, 0.3), 1000, 0.01) };

a[2] = { Ringz.ar(Impulse.ar([3, 5]/2, 0, 0.3), 8000, 0.01) };

a[3] = { Ringz.ar(Impulse.ar([3, 5]*16, 0, 0.3), 5000, 0.01) * LFPulse.kr(0.5, 0, 0.05) };


a.removeLast;

a.removeAt(2);


a.clear;





///////////////////// using patterns - event streams  ///////////////////// 



(

// must have 'out' or 'i_out' argument to work properly

SynthDef("who", { arg freq, gate=1, out=0, ffreq=800, amp=0.1; 

var env;

env = Env.asr(0.01, amp, 0.5);

Out.ar(out, Pan2.ar(

Formant.ar(freq, ffreq, 300, EnvGen.kr(env, gate, doneAction:2)), Rand(-1.0, 1.0))

)

}).add;


)



(

s.boot;

a = NodeProxy.audio(s, 2);

a.fadeTime = 2;

b = NodeProxy.audio(s,2);

b.fadeTime = 3;

)


a.play; // monitor output


// play the pattern silently in b

b.source = Pbind(\instrument, \who, \freq, 500, \ffreq, 700, \legato, 0.02);


// play b out through a:

a.source = b;


// filter b with ring modulation:

a.source = {  b.ar  * SinOsc.ar(SinOsc.kr(0.2, 300, 330))  }; // filter the input of the pattern

a.source = {  b.ar * LFCub.ar([2, 8], add: -0.5)  }; // filter the input of the pattern


a.source = b;


// map b to another proxy

c = NodeProxy.control(s, 1).fadeTime_(1);

c.source = { SinOsc.kr(2, 0, 400, 700) };



// now one can simply embed a control node proxy into an event pattern.

// (this works not for \degree, \midinote, etc.)

// embedding in other patterns it will still return itself.



b.source = Pbind(\instrument, \who, \freq, 500, \ffreq, c, \legato, 0.02);


c.source = { SinOsc.kr(SinOsc.kr(0.2, 0, 10, 10), 0, 400, 700) };


c.source = { LFNoise1.kr(5, 1300, 1500) };

c.source = { MouseX.kr(100, 5500, 1) };


(

b.source = Pbind(

\instrument, \who, 

\freq, Pseq([600, 350, 300],inf),

\legato, 0.1,

\ffreq, Pseq([c, 100, c, 100, 300, 600], inf), // use proxy in a pattern

\dur, Pseq([1, 0.5, 0.75, 0.25] * 0.4, inf),

\amp, Pseq([0.2, 0.2, 0.1, 0.1, 0.2], inf)

);

)


 


b[2] = Pbind(\instrument, \who, \freq, 620, \ffreq, Prand([500,c],inf), \legato, 0.1, \dur, 0.1);

b[3] = Pbind(\instrument, \who, \ffreq, 5000, \freq, Pseq([720, 800],inf), \legato, 0.1, \dur, 0.1, \amp, 0.01);

b[4] = Pbind(\instrument, \who, \freq, Pseq([700, 400],inf), \legato, 0.1, \ffreq, 200);

b[1] = { WhiteNoise.ar([0.01,0.01]) }; 

b[4] = { arg ffreq=800; Resonz.ar(WhiteNoise.ar([1,1]), ffreq, 0.05) }; 



b.map(\ffreq, c); // map the control to the proxy

b.removeLast;

b.removeLast;

a.source = {  b.ar * WhiteNoise.ar(0.1, 1)  }; 

a.source = {  b.ar * WhiteNoise.ar(0.1, 1) + (b.ar * SinOsc.ar(SinOsc.kr(0.01, 0, 50, 330)))  }; 


c.source = { XLine.kr(1900, 10, 10) };


a.clear(10); b.clear(10); c.clear(10); // fade out and clear all (free bus, group and synths)


