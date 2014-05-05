/* Author: 
 */
$(function($){
	
	$.fn.initepas = function() {
		
		
		this.find('.my-modal').on('hidden.bs.modal', function(){
		    $(this).data('bs.modal', null);
		});
		
		// $.fn.editable.defaults.mode = 'inline';
		this.find('a[data-x-editable]').editable();
		
		this.find("a[data-popover]").popover();
		this.find("input[data-datepicker]").datepicker();
		this.find("#datepicker1" ).datepicker();
		this.find(".datepicker" ).datepicker();
		this.find("#datepicker3" ).datepicker();
		
		this.find('#myModal1').on('show', function () {
			$('#myModal2').modal('hide');
		})

		this.find('#myModal2').on('show', function () {
			$('#myModal1').modal('hide');
		})

		this.find('#myModal1').on('hide', function(){
		    $(this).data('modal', null);
		});

		this.find('#myModal2').on('hide', function(){
		    $(this).data('modal', null);
		});

		this.find('#myModal1').on('hidden', function(){
		    $(this).data('modal', null);
		});

		this.find('#myModal2').on('hidden', function(){
		    $(this).data('modal', null);
		});

		this.find('#myModal3').on('hidden.bs.modal', function(){
		    $(this).data('bs.modal', null);
		});
		

		this.find('#myModal4').on('hidden', function(){
		    $(this).data('modal', null);
		});

		this.find('#modal-insert-contract').on('hidden', function(){
		    $(this).data('modal', null);
		});

		this.find('#modal-edit-contract').on('hidden', function(){
		    $(this).data('modal', null);
		});

		this.find('#modal-edit-source-contract').on('hidden', function(){
		    $(this).data('modal', null);
		});

		this.find('#modal-terminate-person').on('hidden', function(){
		    $(this).data('modal', null);
		});

		this.find('#modal-edit-vacationperiod').on('hidden', function(){
		    $(this).data('modal', null);
		});

		this.find('#modal-insert-vacationperiod').on('hidden', function(){
		    $(this).data('modal', null);
		});

		this.find('#modal-absencetype-month').on('hidden', function(){
		    $(this).data('modal', null);
		});


		this.find('#select1').editable(); 
		this.find('#select2').editable(); 
		this.find('#select3').editable(); 
		this.find('#select4').editable(); 
		this.find('#select5').editable(); 
		this.find('#select6').editable(); 
		this.find('#simpleText1').editable(); 
		this.find('#simpleText2').editable();
		this.find('#simpleText3').editable(); 

		this.find('#textComments1').editable({
		    showbuttons: 'bottom'
		}); 
		
		this.find('form[data-reload] :input').on('change', function(e) {
	    	var $form = $(this).closest("form");
	    	var selector = $form.data('reload');
	    	var $target = $(selector);
	    	$target.addClass('reloading');
	    	var $spinner = $('<span class="text-primary" style="position:absolute; z-index: 10"><i class="icon-spinner icon-spin icon-2x"></i</span>').prependTo($target);
	    	var offset = $spinner.offset();
	    	$spinner.offset({top:offset.top + 1, left:offset.left + 250});
	    	var url = $form.prop('action') + '?'+ $form.find(":input").serialize();
	    	$target.load(url + ' '+ selector, function(response, status, request) {
	    		// History.replaceState(null, $('title').text, url);
	    		$target.removeClass('reloading');
	    		$target.initepas();
	    	});
	    });
	    
	    this.find('form[data-reload] input[type=text]').on('input', function(e) {
	    	var $this = $(this);
	    	var autochange_timeout = $this.data('autochange_timeout')
	    	if (autochange_timeout) {
	    		clearTimeout(autochange_timeout);
	    		$this.removeData('autochange_timeout', autochange_timeout);
	    	}
	    	$this.data('autochange_timeout', setTimeout(function() {
	    		$this.trigger('change');
	    	}, 500));
	    });
		
	}
	$('body').initepas();
	
	$('a[data-modal]').click(function(e) {
		var $this = $(this);
		var url = $this.attr('href');
		var $modal = $($this.data('modal'));
		var $modalbody = $modal.modal('show').find('.modal-body');
		$modalbody.load(url, function() {
			$modalbody.initepas();
		});
		e.preventDefault();
	});
});









	


