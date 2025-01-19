import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthenticationRequest } from 'src/app/services/models';
import { AuthenticationService } from 'src/app/services/services';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent {
  authRequest: AuthenticationRequest = { email: '', password: '' };
  errorMsg: Array<string> = [];

  constructor(
    private router: Router,
    private authService: AuthenticationService
  ) {}

  login() {
    this.errorMsg = [];
    this.authService
      .authenticate({
        body: this.authRequest,
      })
      .subscribe({
        next: (res) => {
          //todo save the token
          this.router.navigate(['books']);
          console.log(res);
        },
        error: (err) => {
          console.log(err.error); // Log error response for debugging
          if (
            err.error?.validationErrors &&
            Array.isArray(err.error.validationErrors)
          ) {
            this.errorMsg = err.error.validationErrors; // Directly assign the validation errors
          } else if (err.error?.errorMsg) {
            this.errorMsg.push(err.error.error); // In case there's a generic error message
          } else {
            this.errorMsg.push('An unknown error occurred'); // Fallback error
          }
        },
      });
  }

  register() {
    this.router.navigate(['register']);
  }
}
