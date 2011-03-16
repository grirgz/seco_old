
(
~make_ordered_dictionary = {
	(
		// members

		list: [],
		dict: Dictionary.new,

		// private methods

		rebuild: { arg self;
			self.dict = Dictionary.new;
			self.list.do { arg asso, idx;
				self.dict[asso.key] = idx;
			};
		},

		make_name: { arg self, item, name = nil;
			name.debug("oadd make_name");
			if( name.isNil, { name = item.identityHash.asSymbol });
			name.debug("oadd make_name2");
			name;
		},

		// public methods

		oadd: { arg self, item, name = nil;
			name.debug("oadd name");
			name = self.make_name(item, name);
			self.list = self.list.add( name -> item );
			self.rebuild.value;
			name -> self.dict[name];
		},

		oinsert: { arg self, idx, item, name = nil;
			name = self.make_name(item, name);
			self.list = self.list.insert( idx, name -> item );
			self.rebuild.value;
			name -> self.dict[name];
		},

		odel_by_name: { arg self, name;
			if( self.dict[name].isNil, { Error("odel_by_name: item not found").throw });
			self.list.removeAt( self.dict[name] );
			self.rebuild.value;
		},

		odel: { arg self, idx;
			self.list.removeAt( idx );
			self.rebuild.value;
		},

		oget: { arg self, idx;
			self.list[idx].value;
		},

		oget_by_name: { arg self, name;
			name.dump;
			if( self.dict[name].isNil, { nil }, {
				self.dict[name].debug("db");
				self.list[ self.dict[name] ].value;
			});
		},

		oget_list: { arg self;
			self.list.collect { arg asso; asso.value };
		},

		oget_dict: { arg self;
			var dico = Dictionary.new;
			self.list.do { arg asso; dico[asso.key] = asso.value  };
			dico;
		},

		oinit: { arg self, list;
			self.list = list;
			self.rebuild.value;
		}
	)
};


~iter_viewport = { arg board_size, viewport, fun;
	var abs_point, rel_point, board_rect, view_rect;

	board_rect = Rect.fromPoints( 0@0, board_size );
	view_rect = board_rect.sect(viewport);

	(view_rect.left..view_rect.right).do { arg rx, ax;
		(view_rect.top..view_rect.bottom).do { arg ry, ay;
			fun.( rx @ ry, ax @ ay );
		}
	}
};

~iter_range = { arg size, range, fun;
	var rs = range.x, re = range.y;
	var in_end = min(size, re);
	(rs..in_end).do { arg ri, ai;
		fun.(ri, ai)
	}
};

~iter_lines = { arg board_size, viewport, fun;
	~iter_range.(board_size.y, viewport.top @ viewport.bottom, fun)
};

~iter_rows = { arg board_size, viewport, fun;
	~iter_range.(board_size.x, viewport.left @ viewport.right, fun)
};

~map_matrix = { arg size, viewport, mat_source, mat_dest, fun;
	~iter_viewport.( size, viewport, { arg rp, ap;
		mat_dest[ap.y][ap.x] = fun.( mat_source[ap.y][ap.x] )
	
	});
};

~toggle_value = { arg value;
	if( value.isNil, { value = 0 });
	((value+1) % 2);
};

~toggle_button = { arg button;
	button.value = ((button.value+1) % 2);
};


)

(
~board = 20 @ 25;
~viewport = Rect(5, 10, 3, 4);

~iter_range.value( 10, 5@15, { arg ri, ai; [ri, ai].postln });

~iter_rows.value( ~board, ~viewport, { arg ri, ai; [ri, ai].postln });
~iter_lines.value( ~board, ~viewport, { arg ri, ai; [ri, ai].postln });

~iter_rows.value( ~board, ~viewport, { arg rx, ax;
	~iter_lines.value( ~board, ~viewport, { arg ry, ay;
		[rx@ry, ax@ay].postln
	});
});
)


(
~a = ~make_ordered_dictionary.value;

~a.oadd("bla");
~a.oadd("niah", \poy);
~a.oinsert(0, "haha");
~a.oget_list.postln;
~a.oinsert(0, "hihi", \pol);
~a.oget_list.postln;
~b = ~a.oinsert(1, "hey");
~a.oget_list.postln;
~a.oget_by_name(~b.key).postln;
~a.odel(2);
~a.oget_list.postln;
~a.oget_dict.postln;
~a.odel_by_name(~b.key);
~a.oget_list.postln;
~a.oget_dict.postln;
)
(
// error handling
~a.oget_by_name(\ff)
~a.odel_by_name(\ff)
~a.odel(44)
)
(
~c = [
		\ps_perc -> (
			instr: [ \p_kick, \p_snare ],
			var: [ \vs_rock, \vs_reggae ]
		),
		\ps_melo -> (
			instr: [ \p_bub ],
			var: [\vs_sad, \vs_happy]
		)
	];
~a.oinit( ~c );

~a.oget_list.postln;
~a.oget_dict.postln;
)
