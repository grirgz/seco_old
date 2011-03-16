
// with synthdef (depends on synths defined above):

v = Voicer.new(8, "harpsi");  // 8 voices, all harpsi


// with Instr & MixerChannel:

s = Server.local; s.boot;

m = MixerChannel.new("harpsi", s, 2, 2);

v = Voicer.new(8, Instr.at([\harpsi]), target:m);


// a nested Instr:

(

i = Instr([\harpsi], {

arg freq = 440, gate = 0;

var out;

out = EnvGen.ar(Env.adsr, gate, doneAction:2) *

Pulse.ar(freq, 0.25, 0.25);

[out,out]

});

f = Instr([\test, \rlpf], {

arg audio, ffreq = 500, rq = 0.1;

RLPF.ar(audio, ffreq, rq);

});

)


// If you supply an Instr as an argument, it must be followed by an argument array or nil.

// The Voicer makes a Patch for the inner Instr using the arg array immediately following.

v = Voicer(8, Instr.at([\test, \rlpf]), [\audio, Instr.at([\harpsi]), nil, \ffreq, 5000, \rq, 0.08]);
v = Voicer.new(8, "harpsi");  // uses Server.local

f = Array.fill(5, { 1000.0.rand + 50 });

v.trigger(f);  // play 5 notes

v.release(f);  // release the same


















(

i = Instr([\harpsi], {

arg freq = 440, gate = 0;

var out;

out = EnvGen.ar(Env.adsr, gate, doneAction:2) *

Pulse.ar(freq, 0.25, 0.25);

[out,out]

}/*, [\freq, \amp]*/);

f = Instr([\test, \rlpf], {

arg audio, ffreq = 500, rq = 0.1;

RLPF.ar(audio, ffreq, rq);

});


v = Voicer(8, f, [\audio, i, nil, \ffreq, 5000, \rq, 0.08]);

)


// globalize the filter cutoff

b = v.mapGlobal(\ffreq);


(

SynthDef.new("SinLFO", { // sinewave lfo

arg outbus, freq = 1, phase = 0, mul = 1, add = 0;

ReplaceOut.kr(outbus, SinOsc.kr(freq, phase, mul, add));

}).load(Server.local);

)


l = Synth.new("SinLFO", [\freq, 0.2, \mul, 500, \add, 1400, \outbus, b.index]);


// all notes have the same filter LFO

v.trigger([60, 64, 67].midicps);


v.unmapGlobal(\ffreq);  // LFO stops

v.mapGlobal(\ffreq, b); // set LFO to bus (which is still active)


v.release([60, 64, 67].midicps);

l.free;


m = Array.fill(5, { arg i; MixerChannel("test" ++ (i+1), s) });

n = MixerChannel("essai", s);

z = MixingBoard("test board", nil, m, n); 
