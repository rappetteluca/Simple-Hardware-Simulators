The program 'Simulator' is a trace driven MIPS simulator. It takes no arguments.


*IMPORTANT*: This specific simulator has a 'WINDOWSMODE' variable that affects the formatting at the end of each line (CR LF when true, LF when false).
I thought it was odd that you stressed using a unix based system to test our code yet the text file I downloaded used Windows text formatting(CR LF)

I am unsure whether this was intentional or not so I have added formatting capability for both Windows and Unix, with Windows being the default. 
If you want Unix formatting, set WINDOWSMODE to false and then compile.