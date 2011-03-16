s.boot
~thin = { LPF.ar(Saw.ar(440, 0.1), 3000) ! 2 }.play;
~thin.free;

~fatter = { LPF.ar(Saw.ar(440 * [1, 1.003], 0.1).sum, 3000) ! 2 }.play;
~fatter.free;


(
SynthDef("kickDrum", { arg gate=0;

       var daNoise,daOsc,env1,env2,env3;



       //noise filter cutoff envelope

       //controlls cutoff pitch...0 to 80 Hz

       env1=Env.perc(0.001,1,80,-20);

       //mix-amp envelope

       //controlls overall amplitude...0 to 1

       env2=Env.perc(0.001,1,1,-8);

       //osc-pitch envelope

       //controlls pitch of the oscillator...0 to 80 Hz

       env3=Env.perc(0.001,1,80,-8);



       //Attack noise portion of the sound

       //filter cutoff controlled by env1

       //+20 to move it into the audible

       //spectrum

       daNoise=LPF.ar(WhiteNoise.ar(1),EnvGen.kr(env1,gate)+20);

   //VCO portion of the sound

   //Osc pitch controlled by env3

   //+20 to move the pitch into the

   //audible spectrum

       daOsc=LPF.ar(SinOsc.ar(EnvGen.kr(env3,gate)+20),200);



       //output

       Out.ar(0,Pan2.ar(

                               Mix.ar([daNoise,daOsc]),

                               0, //position

                               //level controlled by env2

                               EnvGen.kr(env2,gate,doneAction: 2)

                       );

                 );

}).load(s);

)

Synth("kickDrum",[\gate,1]);



{ LPF.ar( Ringz.ar( Impulse.ar(1), 60, 0.5 ), 500 ) * 0.25 }.play;



// original: hooray for mono! People should only ever use their left speaker
// in code posted on the list
{ LPF.ar( Ringz.ar( Impulse.ar(1), 60, 0.5 ), 500 ) * 0.25 }.play;

a = { (LPF.ar( Ringz.ar( Impulse.ar(1), 60, 0.5 ), 500 ) * 0.25) ! 2 }.play;

a.free;


a = {
       var sig = LPF.ar( Ringz.ar( Impulse.ar(1), 60, 0.5 ), 500 ),
               cmp = CompanderD.ar(sig, thresh: -20.dbamp, slopeBelow: 1, slopeAbove: 0.3, clampTime: 0.003, relaxTime: 0.08);
       (cmp * (10.dbamp * 0.25)) ! 2
}.play;

a.free;


// freekin' hell, I got some weird error in blendAt
// when trying to plot a multichannel function
// this should have worked but didn't
{
       var sig = LPF.ar( Ringz.ar( Impulse.ar(2), 60, 0.4 ), 500 ),
               cmp = CompanderD.ar(sig, thresh: -20.dbamp, slopeBelow: 1, slopeAbove: 0.3, clampTime: 0.003, relaxTime: 0.08);
       [sig, cmp * 10.dbamp]
}.plot(1.0);


// so let's do it by hand...
b = Buffer.alloc(s, 44100, 2);

{
       var sig = LPF.ar( Ringz.ar( Impulse.ar(2), 60, 0.4 ), 500 ),
               cmp = CompanderD.ar(sig, thresh: -20.dbamp, slopeBelow: 1, slopeAbove: 0.3, clampTime: 0.003, relaxTime: 0.08);
       RecordBuf.ar([sig, cmp * 10.dbamp], b, loop: 0);
       Line.kr(0, 1, 1, doneAction: 2);
       0
}.play;

b.getToFloatArray(action: { |data|
       defer { data.plot2(numChannels: 2, minval: -1, maxval: 1) };
});



(
a = (
     SynthDef(\shaaon,{|out = 0, pan = 0, gate = 1 , freq= #[20,40], k = 40, m= 1000, n=4 ,b= 0.1|
         var sig, env, fil;
       sig = FreeVerb.ar(PinkNoise.ar(SinOsc.kr(0.01,1.1,0.2,0.4),SinOsc.kr(XLine.kr(1,100,10),0.28,Pulse.ar(20.0,0.5),0.01)))
  +Blip.ar(freq,10,SinOsc.ar(SinOsc.kr(2.0,0.5,0.2,b)));
       fil = RHPF.ar(sig,400,Blip.ar(m,10),SinOsc.ar(0.1,1.0,SinOsc.ar(0.1,0.5,0.3,0.41)));
       fil = RHPF.ar(fil,1000,Formant.ar(n,1,2));
     env = EnvGen.ar(Env.adsr(0,1,1,0),gate,doneAction:2);
     Out.ar(out,Pan2.ar(fil*env,pan,SinOsc.ar(k)));
}).send(s);
)

)

h = Synth(\shaaon)
h.set(\gate,0);
h.set(\gate,1);
h.set(\k,40);   // 40 is recomended 
h.set(\m,8000);// recomended ,1000, 8000, 9000
h.set(\b,2.0);  // 0.1 , 2 , 3 , 4 , 5 , etc are recomended.
h.set(\n,5.0);  // 3, 4, recomended
h.pan(\pan,0);
s.boot
(
SynthDef(\pilgrims_poetry,{|gate=1, out=0, pan = 0, g = 1.10, k = 10.23 , l = 0.1, e |
  var sig, env, fil, sig1, eco;
   sig = SinOsc.ar(40,0.5,0.51)+ SinOsc.ar(80,0,SinOsc.kr(l,0,2,1),Pulse.ar(170,0.75))+
         SinOsc.ar(60,1,SinOsc.kr(0.02,0,0.2,1));
   sig1 = PitchShift.ar(sig,1,1,0.3,0.4,SinOsc.ar(SinOsc.kr(320,1.50,1,0.51)));
   eco = FreeVerb.ar(sig1,SinOsc.kr(g),SinOsc.kr(1.51),0.1,SinOsc.ar(k));
   eco = FreeVerb.ar(CombL.ar(eco,0.01,SinOsc.ar(0.0001)),SinOsc.ar(e));
   //fil = RHPF.ar(eco,1000,Saw.ar(0.51));
   env = EnvGen.kr(Env.adsr(0,1,1,0),gate,doneAction:2);
   Out.ar(out,Pan2.ar(eco*env,pan,0.2));
}).send(s);
)
d = Synth(\pilgrims_poetry)
d.set(\gate,0)
d.set(\k, 0.10000)// move the point add numbers
d.set(\g, 0.0000) //  move the point add numbers
d.set(\e, 10.0000)// move the point add numbers
d.set(\pan,0)
) 
s.boot
(
SynthDef(\deborot,{|gate=1, out=0, pan = 0, g = 1.10, k = 10.23 , c= 0.0001, h = 0.02 |
  var sig, env, fil, sig1, eco;
   sig = SinOsc.ar(110,0.5,0.51)+ SinOsc.ar(70,0.25,SinOsc.kr(1,0,2,1),Pulse.ar(170,0.75,SinOsc.kr(0.01,0,2,1)))+
         SinOsc.ar(1300,1,SinOsc.kr(h,0,0.2,1));
   sig1 = PitchShift.ar(sig,0.1,1,0.3,0.4,SinOsc.ar(SinOsc.kr(22,0,1,0.51)));
   eco = FreeVerb.ar(sig1,SinOsc.kr(XLine.kr(0.1,1,20)),SinOsc.kr(0.51),1,SinOsc.ar(k));
   env = EnvGen.kr(Env.adsr(0,1,1,0),gate,doneAction:2);
   fil = RHPF.ar(eco,100,Saw.ar(190));
   Out.ar(out,Pan2.ar(fil*env,pan,0.2));
}).send(s);
)
q = Synth(\deborot)
q.set(\gate,0)        
