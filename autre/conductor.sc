s.boot


(
             SynthDef(\stuck, {|out = 0, freq1, freq2, amp|

                       var impulses, vosim, fof, verb, moog;

                       impulses = Blip.ar(200.15);
                       vosim = VOSIM.ar(impulses, freq1, 3, 0.99);
                       fof = Formlet.ar(vosim, freq2,  0.01, 0.1);
                       verb = FreeVerb.ar(fof.tanh,
                                                               Lag.kr(LFNoise1.kr.range(0.1, 0.9),5),
                                                               Lag.kr(LFNoise1.kr.range(0.3, 0.7),5),
                                                               Lag.kr(LFNoise1.kr.range(0.3, 0.7), 5));
                       moog = BMoog.ar(verb, Lag.kr(LFNoise2.kr.range(600, 800), 10), 0.5, 0);
                       Out.ar(out, moog * amp);
               }).add;

)
(
             SynthDef(\stuck, {|out = 0, freq1, freq2, amp|

                       var impulses, vosim, fof, verb, moog;

                       moog = Blip.ar(freq1)*SinOsc.ar(freq2);
                       Out.ar(out, moog * amp);
               }).add;

)

// ...
(
var con;

con = Conductor.make({|con, les_x, les_amp, shelly_x, shelly_amp, norah_x, norah_amp, juju_x, juju_amp, jorge_x, jorge_amp, ant_x, ant_amp|



                       les_x.spec_(\unipolar);
                       les_amp.spec_(\amp);

// . . .


                       con.synth_(
                               (instrument: \stuck ),
                               [
                                       amp: les_amp,
                                       freq1: [les_x,
                                               ((les_x * (880.cpsmidi - 220.cpsmidi)) + 220.cpsmidi).midicps],
                                       freq2: [les_x,
                                               ((les_x * (111.1.cpsmidi - 880.cpsmidi)) + 111.1.cpsmidi).midicps]
                               ]
                       )

               });
con.show;
)

Quarks.gui
