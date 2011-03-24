Pdef.all
Pdef(\v_snare2).play
Pdef(\p_snare).play




(
~make_player = { arg patfun;
	var get, data, nodeproxy;
	data = {
		var dict = Dictionary.new;
		patfun.defaultArgs.debug("defaultArgs");
		patfun.argNames.do({ arg argName, idx;
			argName.debug("argName");
			idx.debug("idx");
			dict[argName] = patfun.defaultArgs[idx];
		});
		dict.debug("dict");
		dict;
	}.value;
	get = { arg argName;
		argName.debug("get");
		data[argName].debug("get ret");
		data[argName];
	};

	nodeproxy = NodeProxy.audio(s, 2);
	nodeproxy.source = patfun.valueArray( patfun.argNames.collect({ arg argName;
		switch(argName, 
			\stepline, { 
				var repeat = 100;
				Prout({
					var idx;
					repeat.do {
						idx = 0;
						while( { get.(argName)[idx].notNil } , { 
							get.(argName)[idx].debug("argname");
							get.(argName)[idx].yield;
							idx = idx + 1;
						});
					}
				})
			},
			\type, {
				Pif( Pkey(\stepline) > 0 , \note, \rest) // WTF with == ?????
			},
			//default:
			{
				Prout({
					var repeat = 100;
					repeat.do {
						get.(argName).yield;
					}
				})
			}
		);
	}));

			

	(
		patfun: patfun,
		node: nodeproxy,
		data: data,
		get: get,
		set: { arg self, argName, val;
			data[argName] = val;
		},
		getargs: { arg self; patfun.argNames }
	)
};
)

(

~p = ~make_player.({ arg amp=0.1, type, stepline = #[1,1,0,1], freq = 300;
	//arg freq = 300;

	Pbind(
		\freq, freq,
		\stepline, stepline,
		\amp, amp,
		\type, type
		//\type, \note
	)


});

~p.node.play;
//~p.node.source.patternpairs;
)

(
~pp = NodeProxy.audio(s, 2);
	~pp.source = Pbind(
		\freq, 300,
		\stepline,
				Prout({
					var idx;
					var tab = [1,1,0,1];
					100.do {
						idx = 0;
						while( { tab[idx].notNil } , { 
							tab[idx].yield;
							idx = idx + 1;
						});
					}
				}),
		\type, Pif( Pkey(\stepline) > 0 , \note, \rest) // WTF with == ?????
	);
	~pp.play
)



UniqueID.next;
(

		~s = "blsdkfj_l452452";
		~s[ .. ~s.findBackwards("_l")  ]

)
"foobar".findRegexp("o*bar");

{ arg bla; bla+1; }.def.dump










b = NodeProxy.audio(s, 2);

b.source = { PinkNoise.ar(0.2.dup) };
b.play

a = PatternProxy.new;

a.play; // play to hardware output, return a group with synths


// setting the source

a.source = { SinOsc.ar([350, 351.3], 0, 0.2) };


// the proxy has two channels now:

a.numChannels.postln;

a.source = { SinOsc.ar([390, 286] * 1.2, 0, 0.2) };


(
a.source = 
	Pbind(
		\freq, Prout({
			100.do {
				"plop".postln;
				300.yield;
			}
		})
		//\type, \note
	);
)
(
a.source = 
	Pbind(
		\freq, Pfunc({
				"plop".postln;
				300;
		})
		//\type, \note
	);
)
["stepline", "amp", "type", "freq", "bla"].reject({ arg it; ["stepline","amp","type"].includes(it)})
List[\stepline,\amp,\type].includes(\amp)






(
CCResponder.removeAll;
~cc =  CCResponder({ |src,chan,num,value|
		//[src,chan,num,value].debug("==============CCResponder");
		~bla.set( \freq.asSpec.map(value/127) );
	},
	nil,
	nil,
	~cakewalk[\knob][0],
	nil
);


)



(
~bla = Bus.control(s, 1).set(440);
//~bla = NodeProxy.control(s, 1);
//~a = EventPatternProxy.new;
//~a.source = Pbind(
~b = Pbind(
	\freq, ~bla.asMap
);
~b.play;







)
~t.play
~a.map(\freq, ~bla);
~a.play;
(
~bla = NodeProxy.control(s, 1);
~bla.set(300);
//~bla = NodeProxy.control(s, 1);
~a = NodeProxy.audio(s, 2);
//~a.source = Pbind(
~a.source = Pbind(
	\freq, 500
);
~a.play;







)
(
a = NodeProxy.audio(s, 2);
a.source = { arg f=400; SinOsc.ar(f * [1,1.2] * rrand(0.9, 1.1), 0, 0.1) };
c = NodeProxy.control(s, 2);

c.source = { SinOsc.kr(20 * 0.1, 0, 150, 1300) };

a.map(\f, c);
)



(
b = NodeProxy.audio(s, 2);
c = NodeProxy.control(s, 2);
c.source = 300;

b.map(\freq, c); // map the control to the proxy

//b.lag(\freq, 1.5);


b.source = Pbind( \freq, 300, \dur, 1 );
//b.source = { arg freq=400; SinOsc.ar(freq * [1,1.2] * rrand(0.9, 1.1), 0, 0.1) };
b.play;
)
b.set(\freq, rrand(1500, 70));

(
CCResponder.removeAll;
~cc =  CCResponder({ |src,chan,num,value|
		//[src,chan,num,value].debug("==============CCResponder");
		c.source =  \freq.asSpec.map(value/127) ;
	},
	nil,
	nil,
	~cakewalk[\knob][0],
	nil
);


)

// setting controls

a = NodeProxy.audio(s, 2);
a.fadeTime = 2.0;
a.play;

a.source = { arg f=400; SinOsc.ar(f * [1,1.2] * rrand(0.9, 1.1), 0, 0.1) };

a.set(\f, rrand(900, 300));

a.set(\f, rrand(1500, 700));

a.xset(\f, rrand(1500, 700)); // crossfaded setting

a.source = { arg f=400; RLPF.ar(Pulse.ar(f * [1,1.02] * 0.05, 0.5, 0.2), f * 0.58, 0.2) };


// control lags

a.lag(\f, 0.5); // the objects are built again internally and sent to the server.

a.set(\f, rrand(1500, 700));

a.lag(\f, nil);

a.set(\f, rrand(1500, 700));


a.fadeTime = 1.0;












(
b = NodeProxy.audio(s, 2);
c = NodeProxy.control(s, 2);

c.source = { SinOsc.kr(20 * 0.1, 0, 150, 1300) };


b.map(\freq, c); // map the control to the proxy

b.source = Pbind( \freq, 300, \dur, 1 );
b.play;
)
c.source = 300;

(
CCResponder.removeAll;
~cc =  CCResponder({ |src,chan,num,value|
		//[src,chan,num,value].debug("==============CCResponder");
		c.source =  \freq.asSpec.map(value/127) ;
	},
	nil,
	nil,
	~cakewalk[\knob][0],
	nil
);


)
