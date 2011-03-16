(
//redFrik 2011
s.waitForBoot{
       Spec.add(\freq1, #[20, 10000, \exp, 0, 100]);
       Spec.add(\freq2, #[20, 10000, \exp, 0, 200]);
       Spec.add(\freq3, #[20, 10000, \exp, 0, 300]);
       Spec.add(\fmod1, #[0.001, 100, \exp, 0, 0.11]);
       Spec.add(\fmod2, #[0.001, 100, \exp, 0, 0.22]);
       Spec.add(\fmod3, #[0.001, 100, \exp, 0, 0.33]);
       Spec.add(\fmoda1, #[0, 100, \lin, 0, 1]);
       Spec.add(\fmoda2, #[0, 100, \lin, 0, 1]);
       Spec.add(\fmoda3, #[0, 100, \lin, 0, 1]);
       Spec.add(\pmod1, #[0.001, 100, \exp, 0, 0.1]);
       Spec.add(\pmod2, #[0.001, 100, \exp, 0, 0.2]);
       Spec.add(\pmod3, #[0.001, 100, \exp, 0, 0.3]);
       Spec.add(\amod1, #[0.001, 100, \exp, 0, 0.01]);
       Spec.add(\amod2, #[0.001, 100, \exp, 0, 0.02]);
       Spec.add(\amod3, #[0.001, 100, \exp, 0, 0.03]);
       Spec.add(\amoda1, #[0, 10, \lin, 0, 0.05]);
       Spec.add(\amoda2, #[0, 10, \lin, 0, 0.05]);
       Spec.add(\amoda3, #[0, 10, \lin, 0, 0.05]);
       Spec.add(\smod, #[0.001, 100, \exp, 0, 0.13]);
       Spec.add(\smoda, #[0, 100, \lin, 0, 5]);
       Spec.add(\smodm, #[0, 100, \lin, 0, 6]);
       Spec.add(\smodaa, #[0, 100, \lin, 0, 8]);
       Spec.add(\smodmm, #[0, 100, \lin, 0, 50]);
       Spec.add(\cmod, #[0.001, 100, \exp, 0, 1.2]);
       Spec.add(\cmoda, #[0, 10, \lin, 0, 0.6]);
       Spec.add(\room, #[0, 300, \lin, 1, 20]);
       Spec.add(\reverb, #[0, 30, \lin, 0, 5]);
       Spec.add(\damp, #[0, 1, \lin, 0, 1]);
       Spec.add(\input, #[0, 1, \lin, 0, 0.5]);
       Spec.add(\spread, #[0, 100, \lin, 0, 25]);
       Spec.add(\dry, #[0, 1, \lin, 0, 0]);
       Spec.add(\early, #[0, 1, \lin, 0, 1]);
       Spec.add(\tail, #[0, 1, \lin, 0, 1]);
       Ndef(\droneSines).play;
       Ndef(\droneSines, {|freq1= 100, freq2= 200, freq3= 300, fmod1= 0.11, fmod2= 0.22, fmod3= 0.33, fmoda1= 1, fmoda2= 1, fmoda3= 1, pmod1= 0.1, pmod2= 0.2, pmod3= 0.3, amod1= 0.01, amod2= 0.02, amod3= 0.03, amoda1= 0.05, amoda2= 0.05, amoda3= 0.05, smod= 0.13, smoda= 5, smodm= 6, smodaa= 8, smodmm= 50, amp= 0.7, cmod= 1.2, cmoda= 0.6, room= 20, reverb= 5, damp= 1, input= 0.5, spread= 25, dry= 0, early= 1, tail= 1|
               GVerb.ar(Splay.ar(SinOsc.ar([freq1, freq2, freq3]+SinOsc.ar([fmod1, fmod2, fmod3], 0, [fmoda1, fmoda2, fmoda3]), SinOsc.ar([pmod1, pmod2, pmod3], 0, 2pi), SinOsc.ar([amod1, amod2, amod3], 0, [amoda1, amoda2, amoda3])), SinOsc.ar(SinOsc.ar(SinOsc.ar(smod, 0, smoda, smodm), 0, smodaa, smodmm), 0, 1, 1), amp, SinOsc.ar(cmod, 0, cmoda)), room, reverb, damp, input, spread, dry, early, tail);
       });
       Ndef(\droneSines).edit;
       s.meter;
};
)
//Ndef(\droneSines).clear;
Ndef(\droneSines).stop;



//--save a preset
//Ndef(\droneSines).nodeMap.writeArchive("pset1.txt")

//--recall a preset
//Ndef(\droneSines).nodeMap= Object.readArchive("pset1.txt")

//--scramble settings
(
Ndef(\droneSines).controlKeys.do{|k|
       Ndef(\droneSines).set(k, k.asSpec.map(1.0.rand));
};
)



Ndef(\droneSines).nodeMap= Object.readArchive("pset1.txt")
Ndef(\droneSines).nodeMap
Ndef(\droneSines).clear
Ndef(\droneSines).all

Ndef(\droneSines).nodeMap.writeArchive("/home/ggz/pset1.txt")
Object.readArchive("pset1.txt")


(

SynthDef("modsine", 

{ arg freq=320, amp=0.2;

Out.ar(0, SinOsc.ar(freq, 0, amp));

}).send(s);

SynthDef("lfo", 

{ arg rate=2, busNum=0;

Out.kr(busNum, LFPulse.kr(rate, 0, 0.1, 0.2)) 

}).send(s);

)


//start nodes

(

b = Bus.control(s,1);

x = Synth("modsine");

y = Synth.before(x, "lfo", [\busNum, b]);

)


//create some node maps

(

h = NodeMap.new;

h.set(\freq, 800);

h.map(\amp, b);


k = NodeMap.new;

k.set(\freq, 400);

k.unmap(\amp);

)


//apply the maps


h.sendToNode(x); //the first time a new bundle is made

k.sendToNode(x);


h.sendToNode(x); //the second time the cache is used

k.sendToNode(x);


h.set(\freq, 200);


h.sendToNode(x); //when a value was changed, a new bundle is made


//free all

x.free; b.free; y.free;





