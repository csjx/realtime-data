c skeleton subroutine to read directional wave spectra		
		subroutine readspec(filename,spden,spfreq,spdir,
     &nspec,ndir,xd)

		integer nspec,ndir,iitemp
		real spden(100,100),spfreq(100),spdir(100),nd
		character*50 filename      

      
c enter wave spectrum -ASCII format
c reads spectral density in variable "spden(f,d)" with indicies f and d corresponding to
c the frequencies and directions spfreq(f) and spdir(d) respectively


		open(11,file=filename)
		read(11,*) xd
		read(11,*) nspec
		read(11,*) ndir
		if((nspec.gt.100).OR.(ndir.GT.100))then
			write(*,*) 'wave spectrum dimensions too large'
			close(11)
			stop
		endif
		do i=1,nspec
			read(11,*) spfreq(i)
		enddo
		do j=1,ndir
			read(11,*) spdir(j)
		enddo
		read(11,*)iitemp
		if (iitemp.NE.999) then
			write(*,*)'wave spectrum:corrupt file header'
			close (11)
			stop
		endif
		do i=1,nspec
		do j=1,ndir
			read(11,*) spden(i,j)
		enddo
		enddo
		close(11)

		end

