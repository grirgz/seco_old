
(
~mk_gui = { arg seq;
	var env = Environment.make({

		~width = 1110;
		~height = 500;
		~numstep = 16;
		~stepsbut = [];
		~channel = [];
		~label = [];
		~win = nil;

		~seq_model = (
			channel: [
				(
					label: "chan1",
					step: [
						//[label, action event, value]
						["1", [\step, 0, 1], 1],
						["2", [\step, 0, 2], 0],
						["3", [\step, 0, 3], 0],
					]
				),
				(
					label: "chan2",
					step: [
						//[label, action event, value]
						["1", [\step, 1, 1], 0],
						["2", [\step, 1, 2], 0],
						["3", [\step, 1, 3], 1],
					]
				)

			]
		);

		~seq_view = (
			channel: [
				(
					view: "view",
					label: "label",
					step: [
						"button1",
						"button2",
						"button3"
					]
				),
				(
					view: "view",
					label: "label",
					step: [
						"button1",
						"button2",
						"button3"
					]
				)
			]
		);

		~make_window = { arg self;
			var window, buttons, handlers;
			var ul = [];
			window = GUI.window.new("seq", Rect(50, 50, self.width, self.height)).front;
			window.view.decorator = FlowLayout(window.view.bounds); // notice that FlowView refers to w.view, not w

			self.win = window;

		};


		~refresh_step = { arg self, y, x;
			var stepbut;
			stepsbut = self.seq_view.channel[y].step[y];
			stepsbut.states = [

			]


			
		};















		~add_channel = {
			arg self, parent, name;
			var hl, b, txt, blist = [];
			parent = self.win;
			parent.debug("parenttttttttttt");
			hl = GUI.hLayoutView.new(parent, Rect(0,0,self.width+10,60));
			hl.background = Color.rand;
			txt = GUI.staticText.new(hl, Rect(0,0,100, 50));	// no text, just a vertical spacer
			txt.string = " " ++ name;
			self.channel = self.channel.add(hl);
			self.label = self.label.add(txt);
			
		};
		~del_channel = { arg self, chan;
			
			self.channel[chan].remove

		};

		~play = { arg self, chan;
			seq.jouer(chan)
		};
		~stop = { arg self, chan;
			seq.taire(chan)
		};

		~set_chan = { arg self, chan, player;
			seq.set_chan(chan, player)
		};

		~make_handlers = { arg self;

			self.win.view.keyDownAction = { arg view, char, modifiers, u, k; 
				seq.handler([\char, u, modifiers], 1)
			};
			self.ccresp = CCResponder({ |src,chan,num,value|
					"####============".postln;
					[src,chan,num,value].postln;
					~midi[num].postln;
					seq.handler(~midi[num])
				},
				nil, // any source
				nil, // any channel
				nil, // any CC number
				nil // any value
			);
		};

		~show = { arg self;
			self.win = self.make_window.value;
			self.win.debug("winnnnnnnnnnn");
			seq.boardstate.seqmachine.y.do { arg idx;
				var but;
				but = self.add_channel(self.win, "chan"++idx);
				but.do { arg bu, buidx;
					bu.action = { 
						[idx, buidx].debug("## button handler:");
						seq.handler([\step, idx, buidx]) 
						
					}
				};
				self.stepsbut = self.stepsbut.add( but);
			};
			seq.display_data[\stepbuttons] = self.stepsbut;
			self.make_handlers.value;
		}

	});
	env.know = true;
	env;
};

~gui = ~mk_gui.value;
~gui.make_window;
~gui.add_channel(nil, "plop");

)
~gui.del_channel;
(
var window, buttons, handlers;
var ul = [];
var hl, b, txt, blist = [];
var width = 1110, height = 500;
var self, parent, name;
window = GUI.window.new("seq", Rect(50, 50, width, height)).front;
window.view.decorator = FlowLayout(window.view.bounds); // notice that FlowView refers to w.view, not w

hl = GUI.hLayoutView.new(window, Rect(0,0,width+10,60));
hl.background = Color.rand;
txt = GUI.staticText.new(hl, Rect(0,0,100, 50));	// no text, just a vertical spacer
txt.string = " plop";
window.front;
hl.remove
)
GUI.button.browse











(

(
	~boardstate = ["a","b"];
	~channel = ["x","y"];
	~pat = ["f", "g"];
	~writer = { 
		~channel[0] = "c";
	};
	~reader = {
		~pat[0] = ~channel[0];
	};
	~handler = { arg input;
		~writer.value;
	};

	~gui = ["b1", "b2"];

	~add_chan = {
		~gui[0] = "b3";
	};











)


(
~data = [0,0];

~fun1 = {
	~fun2.value;
};
~fun2 = {

}


)
